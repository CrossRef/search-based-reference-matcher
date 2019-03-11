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
import org.crossref.common.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.JsonUtils;
import org.json.JSONException;

/**
 * Main point of entry for performing reference matching. Its logic relies on
 * calling the search engine of the Crossref API to retrieve target document
 * candidates, and using a validation procedure to select the final target
 * document. An instance of an API client is specified in the constructor of
 * this class.
 * 
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class ReferenceMatcher {

    private boolean cacheJournalAbbrevMap = true;
    private final Map<String, String> journalAbbrevMap = new HashMap<>();
    private final CandidateSelector selector;
    private final CandidateValidator validator = new CandidateValidator();
   
    /**
     * Constructor sets apiClient.
     * 
     * @param apiClient CR API client implementation
     */
    public ReferenceMatcher(ICrossRefApiClient apiClient) {
         this.selector = new CandidateSelector(apiClient);
    }
        
    /**
     * Set flag indicating whether or not to cache journal abbreviation map.
     * @param cacheJournalAbbrevMap A flag
     */
    public void setCacheJournalAbbrevMap(boolean cacheJournalAbbrevMap) {
        this.cacheJournalAbbrevMap = cacheJournalAbbrevMap;
    }
    
    /**
     * Get the cached journal abbreviations map.
     * 
     * @return Journal abbreviations map
     */
    public Map<String, String> getJournalAbbrevMap() {
        return journalAbbrevMap;
    }

    /**
     * Initialize the instance based on current state. Currently
     * this just caches the journals depending on a flag.
     */
    public void initialize() {
        if (cacheJournalAbbrevMap) {
            try {
                InputStream is = ReferenceMatcher.class.getResourceAsStream(
                        "journal-abbreviations.txt");
                new BufferedReader(new InputStreamReader(is, "utf-8")).lines()
                        .map(l -> l.trim().split("\t"))
                        .forEach(a -> {journalAbbrevMap.put(a[0], a[1]);});
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
            response.addMatchedLink(ref.getType() == ReferenceType.STRUCTURED ? 
                matchStructured(q, request)
                :
                matchUnstructured(q, request));
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
    private ReferenceLink matchUnstructured(
        ReferenceQuery query, MatchRequest request) {
        
        Reference ref = query.getReference();
        
        List<Candidate> candidates = selector.findCandidates(
            ref, request.getUnstructuredRows(), 
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
    private ReferenceLink matchStructured(
        ReferenceQuery query, MatchRequest request) {
        
        Reference reference = query.getReference();
        
        List<Candidate> candidates = selector.findCandidates(
            reference, request.getStructuredRows(),
            request.getCandidateMinScore(), request.getHeaders());
        
        Candidate candidate = validator.chooseCandidate(reference, 
            candidates, request.getStructuredMinScore());        
        
        String journalNorm = reference.getFieldValue("journal-title");
        if (journalNorm == null) {
            journalNorm = "";
        }
        journalNorm = journalNorm.toLowerCase().replaceAll("[^a-z]", "");
        
        if (journalAbbrevMap.containsKey(journalNorm)) {
            Reference referenceJournalNorm = reference.withField("journal-title",
                    journalAbbrevMap.get(journalNorm));
            candidates = selector.findCandidates(referenceJournalNorm, 
                request.getStructuredRows(),
                    request.getCandidateMinScore(), request.getHeaders());
            
            Candidate candidate2 = validator.chooseCandidate(referenceJournalNorm,
                    candidates, request.getCandidateMinScore());
            
            if (candidate == null) {
                candidate = candidate2;
            } else if (candidate2 != null && candidate2.getValidationScore() 
                    > candidate.getValidationScore()) {
                candidate = candidate2;
            }
        }
        
        return new ReferenceLink(query,
            candidate == null ? null : candidate.getDOI(), 
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
                queries.add(new ReferenceQuery(new Reference((JSONObject) ref)));
            });          
        } catch (JSONException ex) {
            String[] strs = text.split(delim);
            Arrays.asList(strs).stream().forEach(str -> {
                Reference ref;
                try {
                    // Treat as structured ref if JSON string
                    ref = new Reference(new JSONObject(str));
                } catch (JSONException e) {
                    // Otherwise, treat as unstructured ref
                    ref = new Reference(str);
                }
                queries.add(new ReferenceQuery(ref));
            });         
        }
        
        return queries;
    }    
}