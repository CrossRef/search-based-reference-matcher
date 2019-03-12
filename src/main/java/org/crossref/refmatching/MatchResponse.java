package org.crossref.refmatching;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class contains the results of executing matching logic for a given
 * request.
 * 
 * @author Joe Aparo
 * @author Dominika Tkaczyk
 */
public class MatchResponse {
    
    private final MatchRequest request;
    private final List<ReferenceLink> matchedLinks;
    
    public MatchResponse(MatchRequest request) {
        this.matchedLinks = new ArrayList<>();
        this.request = request;
    }
    
    /**
     * Add a matched link to the result.
     * 
     * @param matchedLink The matched link to add
     */
    public void addMatchedLink(ReferenceLink matchedLink) {
        matchedLinks.add(matchedLink);
    }

    /**
     * Get the initial request arguments used to perform the matching.
     * 
     * @return A matching request object
     */
    public MatchRequest getRequest() {
        return request;
    }

    /**
     * Get the list of matched links.
     * 
     * @return A list of matched links
     */
    public List<ReferenceLink> getMatchedLinks() {
        return matchedLinks.subList(0, matchedLinks.size());
    }
    
    public JSONArray toJSON() {
        JSONArray results = new JSONArray();
        matchedLinks.forEach(
                r -> {
                    JSONObject result = new JSONObject();
                    result.put("reference",
                            r.getQuery().getReference().getType().equals(
                                    ReferenceType.STRUCTURED) ?
                            r.getQuery().getReference().getMetadataAsJSON() :
                            r.getQuery().getReference().getFormattedString());
                    result.put("DOI",
                            (r.getDOI() == null) ? JSONObject.NULL : r.getDOI());
                    result.put("score", r.getScore());
                    results.put(result);
                }
        );
        return results;
    }
}