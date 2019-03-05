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
import java.util.stream.Collectors;
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
        
        String data;
            
        switch(request.getInputType()) {
            case FILE: { // Contained in a text file
                data = FileUtils.readFileToString(new File(request.getInputValue()), "UTF-8");
                break;
            }
            default: { // Specified directly as a string
                data = request.getInputValue();
           }
        }

        MatchResponse response = new MatchResponse(request);
        List<ReferenceLink> refs = null;
        
        // Try to interpret data as a JSON array. If it can't be, assume it
        // one or more delimited reference strings.
        try {
            refs = processJsonArray(
                JsonUtils.createJSONArray(data), request);           
        } catch (JSONException ex) {
            refs = processReferenceStringList(
                Arrays.asList(data.split(request.getDataDelimiter())),request);           
        }
        
        refs.forEach(r -> response.addMatch(r));
        
        return response;
    }
    
    /**
     * Process an array of structured references
     * @param refArray An array JSON objects
     * @param request Request details
     * @return A list of reference matches
     */
    private List<ReferenceLink> processJsonArray(
        JSONArray refArray, MatchRequest request) {
        List<JSONObject> refList = new ArrayList<>();
        refArray.forEach(idx -> {
            refList.add((JSONObject) refArray.get((Integer) idx));
        });

        return refList.parallelStream().map(ref -> {
            return match(ref, request.getCandidateMinScore(), 
                request.getStructuredMinScore(), request.getStructuredRows(), 
                request.getMailTo(), request.getHeaders());
        }).collect(Collectors.toList());
    }
    
    /**
     * Process a list of reference strings, some unstructured, some structured.
     * 
     * @param refStrings List of strings to process
     * @param request Request details
     * @return A list of reference matches
     */
    private List<ReferenceLink> processReferenceStringList(
        List<String> refStrings, MatchRequest request) {
        
        return refStrings.parallelStream().map(s -> {
            try {
                JSONObject refObject = new JSONObject(s);
                return match(refObject, request.getCandidateMinScore(), 
                    request.getStructuredMinScore(), request.getStructuredRows(), 
                    request.getMailTo(), request.getHeaders());
            } catch (JSONException ex) {
                // OK, not JSON object - assume string
                return match(new UnstructuredReference(s), request.getCandidateMinScore(), 
                    request.getUnstructuredMinScore(), request.getUnstructuredRows(), 
                    request.getMailTo(), request.getHeaders());
            }
        }).collect(Collectors.toList());
    }

    /**
     * Perform a match given an unstructured reference string.
     * 
     * @param refString The unstructured reference string
     * @param candidateMinScore Minimum selection score
     * @param unstructuredMinScore Minimum validation score
     * @param rows Number of rows to select for consideration
     * @return A match object
     */
    private ReferenceLink match(
        UnstructuredReference reference, double candidateMinScore, 
        double unstructuredMinScore, int rows, String emailTo, Map<String, String> headers) {
        
        String refString = reference.getString();
        
        List<Candidate> candidates = selector.findCandidates(
            refString, rows, candidateMinScore, emailTo, headers);
        Candidate candidate = validator.chooseCandidate(reference, candidates, unstructuredMinScore);       
         
        return new ReferenceLink(
            refString, candidate == null ? null : candidate.getDOI(), 
            candidate == null ? 0.0 : candidate.getValidationScore());
    }

    /**
     *  Perform a match given a structured JSON object reference.
     * 
     * @param reference The structured reference to match
     * @param candidateMinScore Minimum selection score
     * @param structuredMinScore Minimum validation score
     * @param rows Number of rows to select for consideration
     * @return A match object
     */
    private ReferenceLink match(
        JSONObject reference, double candidateMinScore, 
        double structuredMinScore, int rows, String emailTo, Map<String, String> headers) {
        
        List<Candidate> candidates = selector.findCandidates(
            reference.toString(), rows, candidateMinScore, emailTo, headers);
            
        Candidate candidate = validator.chooseCandidate(new StructuredReference(reference), candidates, structuredMinScore);        
        
        String journalNorm = 
            reference.optString("journal-title").toLowerCase().replaceAll("[^a-z]", "");
        
        if (journals.containsKey(journalNorm)) {
            reference.put("journal-title", journals.get(journalNorm));
            candidates = selector.findCandidates(
                reference.toString(), rows, candidateMinScore, emailTo, headers);
            
            Candidate candidate2 = validator.chooseCandidate(new StructuredReference(reference), candidates, structuredMinScore);
            if (candidate == null) {
                candidate = candidate2;
            }
            if (candidate2.getValidationScore() > candidate.getValidationScore()) {
                candidate = candidate2;
            }
        }
        
        return new ReferenceLink(
            reference.toString(), candidate == null ? null : candidate.getDOI(), 
            candidate == null ? 0.0 : candidate.getValidationScore());
    }
}
