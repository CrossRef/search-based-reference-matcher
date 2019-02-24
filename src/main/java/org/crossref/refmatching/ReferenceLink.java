package org.crossref.refmatching;

/**
 * This represents a matched reference.
 * 
 * @author joe.aparo
 */
public class ReferenceLink {
    private String reference;
    private String doi;
    private double score;

    public ReferenceLink(String reference, String doi, double score) {
        this.reference = reference;
        this.doi = doi;
        this.score = score;
    }

    /**
     * Get the original reference that the item matched on.
     * @return A String representing the reference
     */
    public String getReference() {
        return reference;
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
