package org.crossref.refmatching;

/**
 * This represents a matched reference.
 * 
 * @author joe.aparo
 */
public class ReferenceLink {
    private ReferenceQuery query;
    private String doi;
    private double score;

    public ReferenceLink(ReferenceQuery query, String doi, double score) {
        this.query = query;
        this.doi = doi;
        this.score = score;
    }

    /**
     * Get the original reference query that the item matched on.
     * @return A String representing the reference
     */
    public ReferenceQuery getQuery() {
        return query;
    }

    /**
     * Get the DOI of the matched item.
     * @return A DOI
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Get the matching confidence score.
     * @return A score value
     */
    public double getScore() {
        return score;
    }
}
