package org.crossref.refmatching;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
     * Main method for performing reference matching.
     * 
     * @param request Request parameters
     * 
     * @return A matching response
     * @throws IOException 
     */
    public MatchResponse match(MatchRequest request) throws IOException {
        String data;
            
        switch(request.getInputType()) {
            case FILE: { // Contained in a text file
                data = FileUtils.readFileToString(
                        new File(request.getInputValue()), "UTF-8");
                break;
            }
            default: { // Specified directly as a string
                data = request.getInputValue();
           }
        }
        
        List<Reference> references = parseInput(data, request.getDataDelimiter());

        MatchResponse response = new MatchResponse(request);
        List<ReferenceLink> links = references.parallelStream().map(
            r -> {
                if (r.getType().equals(ReferenceType.STRUCTURED)) {
                    return matchStructured(r, request.getCandidateMinScore(),
                        request.getStructuredMinScore(),
                        request.getStructuredRows(), request.getMailTo(),
                        request.getHeaders());
                } else {
                    return matchUnstructured(r, request.getCandidateMinScore(),
                        request.getUnstructuredMinScore(),
                        request.getUnstructuredRows(), request.getMailTo(),
                        request.getHeaders());
                }
            }
        ).collect(Collectors.toList());
        
        links.forEach(r -> response.addMatchedLink(r));
        return response;
    }
    
    private List<Reference> parseInput(String data, String delimiter) {
        try {
            JSONArray arr = JsonUtils.createJSONArray(data);
            List<Object> refs = StreamSupport.stream(arr.spliterator(), true)
                                    .collect(Collectors.toList());
            return refs.parallelStream()
                        .map(s -> (s instanceof String)
                                ? new Reference((String) s)
                                : new Reference((JSONObject) s))
                        .collect(Collectors.toList());
        } catch (JSONException ex) {
            List<String> strs = Arrays.asList(data.split(delimiter));
            return strs.parallelStream()
                    .map(s -> {
                        try {
                            JSONObject refObject = new JSONObject(s);
                            return new Reference(refObject);
                        } catch (JSONException ex1) {
                            return new Reference(s);
                        }
                        }
                    ).collect(Collectors.toList());
        }
    }

    private ReferenceLink matchUnstructured(Reference reference,
            double candidateMinScore, double unstructuredMinScore, int rows,
            String mailTo, Map<String, String> headers) {
        List<Candidate> candidates = selector.findCandidates(reference, rows,
                candidateMinScore, mailTo, headers);
        Candidate candidate = validator.chooseCandidate(reference, candidates,
                unstructuredMinScore);       
         
        return new ReferenceLink(reference,
                candidate == null ? null : candidate.getDOI(), 
                candidate == null ? 0.0 : candidate.getValidationScore());
    }

    private ReferenceLink matchStructured(Reference reference,
            double candidateMinScore, double structuredMinScore, int rows,
            String mailTo, Map<String, String> headers) {
        
        List<Candidate> candidates = selector.findCandidates(reference, rows,
                candidateMinScore, mailTo, headers);
        Candidate candidate = validator.chooseCandidate(reference, candidates,
                structuredMinScore);        
        
        String journalNorm = reference.getFieldValue("journal-title");
        if (journalNorm == null) {
            journalNorm = "";
        }
        journalNorm = journalNorm.toLowerCase().replaceAll("[^a-z]", "");
        
        if (journalAbbrevMap.containsKey(journalNorm)) {
            Reference referenceJournalNorm = reference.withField("journal-title",
                    journalAbbrevMap.get(journalNorm));
            candidates = selector.findCandidates(referenceJournalNorm, rows,
                    candidateMinScore, mailTo, headers);
            Candidate candidate2 = validator.chooseCandidate(referenceJournalNorm,
                    candidates, structuredMinScore);
            
            if (candidate == null) {
                candidate = candidate2;
            } else if (candidate2 != null && candidate2.getValidationScore() 
                    > candidate.getValidationScore()) {
                candidate = candidate2;
            }
        }
        
        return new ReferenceLink(reference,
                candidate == null ? null : candidate.getDOI(), 
                candidate == null ? 0.0 : candidate.getValidationScore());
    }

}