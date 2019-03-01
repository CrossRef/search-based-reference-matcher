package org.crossref.refmatching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.LogUtils;
import org.crossref.common.utils.Timer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class for identifying reference candidates for matching.
 * 
 * @author Dominika Tkaczyk
 */
public class CandidateSelector {
    private final ICrossRefApiClient apiClient;
    private final Logger log = LogUtils.getLogger();
    
    public CandidateSelector(ICrossRefApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Find candidate journals for matching.
     * 
     * @param refString The reference string to perform selection.
     * @param rows The number of rows to select for consideration
     * @param minScore The minimum score to consider a row a candidate
     * 
     * @return A list of reference candidates
     */
    public List<Candidate> findCandidates(String refString, int rows, double minScore, String mailTo) {
        if (StringUtils.isEmpty(refString)) {
            return new ArrayList<>();
        }

        JSONArray candidates = searchWorks(refString, rows, mailTo);
        return selectCandidates(refString, candidates, minScore);
    }

    private JSONArray searchWorks(String refString, int rows, String mailTo) {
        
        // Invoke the client
        String worksJson;
        try {
            log.debug("API search for: " + refString);
        
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("rows", rows);
            args.put("query.bibliographic", refString);
            if (!StringUtils.isEmpty(mailTo)) { // polite support
                args.put("mailto", mailTo);
            }
            
            // Invoke client for items
            Timer timer = new Timer();
            timer.start();
            
            JSONArray arr = apiClient.getWorks(args);
            
            timer.stop();
            
            log.debug("apiClient.getWorks: " + timer.elapsedMs()); 
            
            return arr;
            
        } catch (IOException ex) {
            log.error("Error calling api client: " + ex.getMessage(), ex);
            return new JSONArray();
        }
    }

    private List<Candidate> selectCandidates(String refString, JSONArray items, double minScore) {
        List<Candidate> candidates = new ArrayList<>();
	if (items == null) {
	    return candidates;
	}
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (candidates.isEmpty()) {
                candidates.add(new Candidate(item));
            } else if (item.getDouble("score") / refString.length() >= minScore) {
                candidates.add(new Candidate(item));
            } else {
                break;
            }
        }
        return candidates;
    }
}
