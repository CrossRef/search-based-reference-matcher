package org.crossref.refmatching;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.crossref.common.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.JsonUtils;
import org.json.JSONException;

/**
 * Main point of entry for performing reference matching. Its logic
 * relies on calling the CrossRef service API to retrieve candidates to
 * consider for a match. An instance of an API client is specified in the
 * constructor of this class.
 * 
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class ReferenceMatcher {
    private final Map<String, String> journals = new HashMap<>();
    private final Logger logger = LogUtils.getLogger();
    private final CandidateSelector selector;
    private final CandidateValidator validator = new CandidateValidator();
    private boolean cacheJournals = true;
   
    /**
     * Constructor sets apiClient.
     * @param apiClient CR API client implementation
     */
    public ReferenceMatcher(ICrossRefApiClient apiClient) {
         this.selector = new CandidateSelector(apiClient);
    }
        
    /**
     * Set flag indicating whether or not to cache journals.
     * @param cacheJournals A flag
     */
    public void setCacheJournals(boolean cacheJournals) {
        this.cacheJournals = cacheJournals;
    }
    
    /**
     * Get the list of cached journals.
     * @return A map of journal entries
     */
    public Map<String, String> getJournals() {
        return journals;
    }
        
    /**
     * Initialize the instance based on current state. Currently
     * this just caches the journals depending on a flag.
     */
    public void initialize() {
        if (cacheJournals) {
            try {
                InputStream is = ReferenceMatcher.class.getResourceAsStream("journal-abbreviations.txt");

                new BufferedReader(new InputStreamReader(is, "utf-8")).lines()
                    .map(l -> l.trim().split("\t"))
                    .forEach(a -> {journals.put(a[0], a[1]);});
            } catch (IOException ex) {
                LogUtils.getLogger().warn("Error caching journal entries", ex);
            }
        }
    }
    
    /**
     * Main method for performing a reference match.
     * 
     * @param request Request parameters
     * @return A match response
     * @throws IOException 
     */
    public MatchResponse match(MatchRequest request) throws IOException {
        
        // If either a file or a string has been specified for textual input
        // data, parse the text into reference queries and add them to the request
        if (request.getInputType() == InputType.FILE) {
            convertTextToQueries(
                FileUtils.readFileToString(new File(request.getInputValue()), "UTF-8"), 
                request.getDataDelimiter()).forEach(q -> request.addQuery(q));
        } else if (request.getInputType() == InputType.STRING) {
            convertTextToQueries(request.getInputValue(), 
                request.getDataDelimiter()).forEach(q -> request.addQuery(q));
        }

        MatchResponse response = new MatchResponse(request);
        
        // Process the queries, which may be a mix of structured/unstructured
        request.getQueries().parallelStream().forEach(q -> {
            Reference ref = q.getReference();
            response.addMatch(ref.getType() == ReferenceType.STRUCTURED ? 
                matchStructuredReference(q, request)
                :
                matchUnstructuredReference(q, request));
        });
                
        return response;
    }
        
    /**
     * Perform a match given an unstructured reference string.
     * 
     * @param query The unstructured reference query to match
     * @param request Match request containing query
     * @return A match object
     */
    private ReferenceLink matchUnstructuredReference(
        ReferenceQuery query, MatchRequest request) {
        
        Reference ref = query.getReference();
        
        List<Candidate> candidates = selector.findCandidates(
            ref.getString(), request.getUnstructuredRows(), 
            request.getCandidateMinScore(), request.getHeaders());
        
        Candidate candidate = validator.chooseCandidate(
            ref, candidates, request.getUnstructuredMinScore());       
         
        return new ReferenceLink(
            query, candidate == null ? null : candidate.getDOI(), 
            candidate == null ? 0.0 : candidate.getValidationScore());
    }

    /**
     *  Perform a match given a structured JSON object reference.
     * 
     * @param query The structured reference query to match
     * @param request Match request containing query
     * @return A match object
     */
    private ReferenceLink matchStructuredReference(
        ReferenceQuery query, MatchRequest request) {
        StructuredReference ref = (StructuredReference) query.getReference();
        
        List<Candidate> candidates = selector.findCandidates(
            ref.getString(), request.getStructuredRows(), 
            request.getCandidateMinScore(), request.getHeaders());
            
        Candidate candidate = validator.chooseCandidate(
            ref, candidates, request.getStructuredMinScore());        
        
        JSONObject refObj = new JSONObject(ref.getMap());
        
        String journalNorm = 
            refObj.optString("journal-title").toLowerCase().replaceAll("[^a-z]", "");
        
        if (journals.containsKey(journalNorm)) {
            refObj.put("journal-title", journals.get(journalNorm));
            
            StructuredReference ref2 = new StructuredReference(refObj);
            
            candidates = selector.findCandidates(
                ref2.getString(), request.getStructuredRows(), 
                request.getCandidateMinScore(), request.getHeaders());
            
            Candidate candidate2 = validator.chooseCandidate(
                ref2, candidates, request.getStructuredMinScore());
            
            if (candidate == null) {
                candidate = candidate2;
            }
            if (candidate2.getValidationScore() > candidate.getValidationScore()) {
                candidate = candidate2;
            }
        }
        
        return new ReferenceLink(
            query, candidate == null ? null : candidate.getDOI(), 
            candidate == null ? 0.0 : candidate.getValidationScore());
    }
    
    /**
     * Parse a string to derive a list of reference query objects.
     * 
     * @param text Input string, which may represent a JSONArray, or a
     *      collection of delimited string references, each of which may be
     *      a structured JSONObject, or an unstructured string.
     * @param delim Delimiter used to separate reference values
     * @return A list of reference query objects, parsed from the input string
     */
    private List<ReferenceQuery> convertTextToQueries(String text, String delim) {
        List<ReferenceQuery> queries = new ArrayList<>();
        
        try {
            JSONArray arr = JsonUtils.createJSONArray(text);
            arr.forEach(ref -> {
                queries.add(new ReferenceQuery(new StructuredReference((JSONObject) ref)));
            });          
        } catch (JSONException ex) {
            String[] strs = text.split(delim);
            Arrays.asList(strs).stream().forEach(str -> {
                Reference ref;
                try {
                    // Treat as structured ref if JSON string
                    ref = new StructuredReference(new JSONObject(str));
                } catch (JSONException e) {
                    // Otherwise, treat as unstructured ref
                    ref = new UnstructuredReference(str);
                }
                queries.add(new ReferenceQuery(ref));
            });         
        }
        
        return queries;
    }
}
