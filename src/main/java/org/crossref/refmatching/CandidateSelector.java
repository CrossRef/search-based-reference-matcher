package org.crossref.refmatching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.EncodeUtils;
import org.crossref.common.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Class for identifying reference candidates for matching.
 * 
 * @author Dominika Tkaczyk
 */
public class CandidateSelector {
    private final double minScore;
    private final int rows;
    private final ICrossRefApiClient apiClient;
    private final Logger log = LogUtils.getLogger();
    
    public CandidateSelector(ICrossRefApiClient apiClient, double minScore, int rows) {
        this.minScore = minScore;
        this.rows = rows;
        this.apiClient = apiClient;
    }

    /**
     * Find candidate journals for matching.
     * 
     * @param refString The reference string to perform selection.
     * @return A list of reference candidates
     */
    public List<Candidate> findCandidates(String refString) {
        if (StringUtils.isEmpty(refString)) {
            return new ArrayList<>();
        }

        JSONArray candidates = searchWorks(refString);
        return selectCandidates(refString, candidates);
    }

    private JSONArray searchWorks(String refString) {
        
        // Invoke the client
        String worksJson;
        try {
            log.debug("API search for: " + refString);
        
            Map<String, Object> args = new LinkedHashMap<String, Object>();
            args.put("rows", rows);
            args.put("query.bibliographic", EncodeUtils.urlEncode(refString));
            
            worksJson = apiClient.getWorks(args);
        } catch (IOException ex) {
            log.error("Error calling api client: " + ex.getMessage(), ex);
            return new JSONArray();
        }
        
        // Parse the response
        try {
	    JSONObject json = new JSONObject(worksJson);
            return json.getJSONObject("message").optJSONArray("items");
	} catch (JSONException ex) {
            log.error("Error parsing json string: " + worksJson, ex);
	    return new JSONArray();
	}
    }

    private List<Candidate> selectCandidates(String refString, JSONArray items) {
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
