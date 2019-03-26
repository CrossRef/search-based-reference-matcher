package org.crossref.refmatching;

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
    
    public MatchResponse(MatchRequest request, List<ReferenceLink> matchedLinks) {
        this.matchedLinks = matchedLinks;
        this.request = request;
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
                            r.getReferenceData().getReference().getType().equals(
                                    ReferenceType.STRUCTURED) ?
                            r.getReferenceData().getReference()
                                    .getMetadataAsJSON() :
                            r.getReferenceData().getReference()
                                    .getFormattedString());
                    result.put("DOI",
                            (r.getDOI() == null) ? JSONObject.NULL : r.getDOI());
                    result.put("score", r.getScore());
                    results.put(result);
                }
        );
        return results;
    }
}