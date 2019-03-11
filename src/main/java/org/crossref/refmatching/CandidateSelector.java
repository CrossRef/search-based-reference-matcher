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
 * Class for selecting target document candidates from the corpus.
 * 
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class CandidateSelector {
    
    private final ICrossRefApiClient apiClient;
    private final Logger log = LogUtils.getLogger();
    
    public CandidateSelector(ICrossRefApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Select candidate target items.
     * 
     * @param reference The reference to match
     * @param rows The number of search items to consider as candidates
     * @param minScore The minimum relevance score to consider a search item
     * a candidate
     * @param mailTo Polite mailTo
     * @param headers Additional headers to pass in the search request
     * 
     * @return A list of candidates
     */
    public List<Candidate> findCandidates(Reference reference, int rows,
            double minScore, Map<String, String> headers) {
        String query = getQuery(reference);
        
        if (StringUtils.isEmpty(query)) {
            return new ArrayList<>();
        }

        JSONArray candidates = searchWorks(query, rows, headers);
        return selectCandidates(query, candidates, minScore);
    }

    private JSONArray searchWorks(String refString, int rows,
            Map<String, String> headers) {
        try {
            log.debug("API search for: " + refString);
        
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("rows", rows);
            args.put("query.bibliographic", refString);
            
            // Invoke client for items
            Timer timer = new Timer();
            timer.start();
            JSONArray arr = apiClient.getWorks(args, headers);
            timer.stop();
            
            log.debug("apiClient.getWorks: " + timer.elapsedMs()); 
            
            return arr;
            
        } catch (IOException ex) {
            log.error("Error calling api client: " + ex.getMessage(), ex);
            return new JSONArray();
        }
    }

    private List<Candidate> selectCandidates(String refString, JSONArray items,
            double minScore) {
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

    private String getQuery(Reference reference) {
        if (reference.getType().equals(ReferenceType.UNSTRUCTURED)) {
            return reference.getFormattedString();
        }
        StringBuilder sb = new StringBuilder(500);
        for (String key : new String[]{"author", "article-title", "journal-title",
            "series-title", "volume-title", "year", "volume", "issue",
            "first-page", "edition", "ISSN"}) {
            
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(reference.getFieldValue(key) == null ?
                    "" : reference.getFieldValue(key));
        }
        return sb.toString().replaceAll(" +", " ").trim();
    }
    
}