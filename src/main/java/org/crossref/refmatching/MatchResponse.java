package org.crossref.refmatching;

import java.util.ArrayList;
import java.util.List;

/**
 * This contains the results of executing matching logic for a given request.
 * 
 * @author joe.aparo
 */
public class MatchResponse {
    private MatchRequest request;
    List<Match> matches = new ArrayList<Match>();
    
    public MatchResponse(MatchRequest request) {
        this.request = request;
    }
    
    /**
     * Add a match to the result.
     * @param match The match to add
     */
    public void addMatch(Match match) {
        matches.add(match);
    }

    /**
     * Get the initial request arguments used to perform the match.
     * @return A match request object
     */
    public MatchRequest getRequest() {
        return request;
    }

    /**
     * Get the list of matches.
     * @return A match list
     */
    public List<Match> getMatches() {
        return matches.subList(0, matches.size());
    }
}