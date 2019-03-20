package org.crossref.refmatching;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.crossref.common.utils.LogUtils;
import org.crossref.common.rest.api.ICrossRefApiClient;

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
     * 
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
     * @param request Request object
     * @return A match response
     * @throws IOException 
     */
    public MatchResponse match(MatchRequest request) throws IOException {
        MatchResponse response = new MatchResponse(request);
        
        // Process the references, which may be a mix of structured/unstructured
        List<ReferenceLink> links = request.getReferences()
                .parallelStream().map(q ->
                        q.getReference().getType() == ReferenceType.STRUCTURED ?
                                matchStructured(q, request) :
                                matchUnstructured(q, request))
                .collect(Collectors.toList());
        links.stream().forEachOrdered(q -> {response.addMatchedLink(q);});

        return response;
    }
        
    /**
     * Match an unstructured reference.
     * 
     * @param query The unstructured reference
     * @param request Match request
     * @return Reference link
     */
    protected ReferenceLink matchUnstructured(ReferenceData query,
            MatchRequest request) {
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
     * Match a structured reference.
     * 
     * @param query The structured reference
     * @param request Match request
     * 
     * @return Reference link
     */
    protected ReferenceLink matchStructured(ReferenceData query,
            MatchRequest request) {
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
  
}