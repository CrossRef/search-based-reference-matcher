package org.crossref.refmatching;

/**
 * The class represents a matched reference.
 * 
 * @author Joe Aparo
 */
public class ReferenceLink {
    
    private final ReferenceData query;
    private final String doi;
    private final double score;

    public ReferenceLink(ReferenceData query, String doi, double score) {
        this.query = query;
        this.doi = doi;
        this.score = score;
    }

    /**
     * Get the original reference query that the item matched on.
     * @return A String representing the reference
     */
    public ReferenceData getQuery() {
        return query;
    }

    /**
     * Get the DOI of the target document.
     * 
     * @return A DOI
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Get the matching confidence score.
     * 
     * @return A score value
     */
    public double getScore() {
        return score;
    }

}