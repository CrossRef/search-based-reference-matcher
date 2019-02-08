package org.crossref.refmatching;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.crossref.common.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.json.JSONException;

/**
 * Main point of entry for performing a reference match. Its logic
 * relies on calling the CrossRef service API. This is made possible by
 * providing an instance of a client interface for that service during the
 * construction of an instance of this class.
 * 
 * @author Dominika Tkaczyk
 */
public class ReferenceMatcher {
    /** Default API service scheme. Should be "http" or "https" */
    public static final String DEFAULT_API_SCHEME = "https";
     /** Default API service host name or IP */
    public static final String DEFAULT_API_HOST = "api.crossref.org";
     /** Default API service port. A value of 0 signifies no port, i.e. the default for the scheme */
    public static final int DEFAULT_API_PORT = 0; // assume no port
    
    private static final int STR_ROWS = 100;
    private static final int UNSTR_ROWS = 20;
    
    private final Map<String, String> journals = new HashMap<>();
    private final Logger logger = LogUtils.getLogger();
    
    private boolean cacheJournals = false;
    public void setCacheJournals(boolean cacheJournals) {
        this.cacheJournals = cacheJournals;
    }

    private ICrossRefApiClient apiClient;
    
    public ReferenceMatcher(ICrossRefApiClient apiClient) {
         this.apiClient = apiClient;       
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
        MatchResponse response = new MatchResponse(request);
        
        switch(request.getInputType()) {
            case JSON_FILE: {
                String inputData = FileUtils.readFileToString(
                    new File(request.getInputFileName()), "UTF-8");
                
                JSONArray dataset = new JSONArray(inputData);

                List<Candidate> matched = StreamSupport.stream(dataset.spliterator(), true)
                    .map(s -> (s instanceof String)
                    ? match((String) s, request.getCandidateMinScore(), 
                    request.getUnstructuredMinScore(), UNSTR_ROWS)
                    : match((JSONObject) s, request.getCandidateMinScore(), request.getStructuredMinScore(), STR_ROWS))
                    .collect(Collectors.toList());

                IntStream.range(0, dataset.length()).mapToObj(i -> new Match(
                    dataset.get(i), 
                    (matched.get(i) == null ? null : matched.get(i).getDOI()), 
                    (matched.get(i) == null ? 0.0 : matched.get(i).getValidationScore()))).forEach(m -> response.addMatch(m));
                break;
            }
            case TEXT_FILE: {
                List<String> references = FileUtils.readLines(
                    new File(request.getInputFileName()), "UTF-8");
                
                List<Candidate> matched = references.parallelStream()
                    .map(s -> match((String) s, request.getCandidateMinScore(), 
                    request.getUnstructuredMinScore(), UNSTR_ROWS))
                    .collect(Collectors.toList());
                
                IntStream.range(0, references.size()).mapToObj(i -> new Match(
                    references.get(i), 
                    (matched.get(i) == null ? null : matched.get(i).getDOI()), 
                    (matched.get(i) == null ? 0.0 : matched.get(i).getValidationScore()))).forEach(m -> response.addMatch(m));
                
                break;
            }
            default: { // STRING
                // Attempt to interpret given string as JSON
                JSONObject refObject = null;
                
                try {
                    refObject = new JSONObject(request.getRefString());
                } catch (JSONException ex) {
                    
                }
                
                Candidate m = (refObject != null) ? 
                    match(
                        refObject, request.getCandidateMinScore(), 
                        request.getStructuredMinScore(), STR_ROWS)
                    :
                    match(
                        request.getRefString(), request.getCandidateMinScore(), 
                        request.getUnstructuredMinScore(), UNSTR_ROWS);
                
                response.addMatch(new Match(
                    request.getRefString(), m == null ? null : m.getDOI(), 
                    m == null ? 0.0 : m.getValidationScore()));
            }
        }
        
        return response;
    }
    
    private Candidate match(String refString, double candidateMinScore, double unstructuredMinScore, int rows) {
        logger.debug(String.format("Matching reference: %s", refString));
        
        CandidateSelector selector = new CandidateSelector(apiClient, candidateMinScore, rows);
        CandidateValidator validator = new CandidateValidator(unstructuredMinScore);
        
        List<Candidate> candidates = selector.findCandidates(refString);
        Candidate candidate = validator.chooseCandidate(refString, candidates);       
         
        logMatch(candidate);

        return candidate;
    }

    private Candidate match(
        JSONObject reference, double candidateMinScore, double structuredMinScore, int rows) {
        
        logger.debug(String.format("Matching reference: %s", reference));
        
        CandidateSelector selector = new CandidateSelector(apiClient, candidateMinScore, rows);
        CandidateValidator validator = new CandidateValidator(structuredMinScore);
        
        List<Candidate> candidates = selector.findCandidates(reference.toString());
        Candidate candidate = validator.chooseCandidate(new StructuredReference(reference), candidates);        
        
        String journalNorm = 
            reference.optString("journal-title").toLowerCase().replaceAll("[^a-z]", "");
        
        if (journals.containsKey(journalNorm)) {
            reference.put("journal-title", journals.get(journalNorm));
            candidates = selector.findCandidates(reference.toString());
            Candidate candidate2 = validator.chooseCandidate(new StructuredReference(reference), candidates);
            if (candidate == null) {
                return candidate2;
            }
            if (candidate2 == null) {
                return candidate;
            }
            if (candidate2.getValidationScore() > candidate.getValidationScore()) {
                return candidate2;
            }
        }
        
        logMatch(candidate);
        
        return candidate;
    }

    /**
     * Get the list of cached journals.
     * @return A map of journal entries
     */
    public Map<String, String> getJournals() {
        return journals;
    }
    
    private void logMatch(Candidate candidate) {
        
        if (candidate != null) {
            logger.debug(String.format("Reference matched to DOI: %s", candidate.getDOI()));
        } else {
            logger.debug("No matching reference.");
        }        
    }
}
