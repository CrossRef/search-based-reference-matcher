package org.crossref.refmatching;

/**
 * The class represents a matched reference.
 * 
 * @author Joe Aparo
 */
public class ReferenceLink {
    
    private final Reference reference;
    private final String doi;
    private final double score;

    public ReferenceLink(Reference reference, String doi, double score) {
        this.reference = reference;
        this.doi = doi;
        this.score = score;
    }

    /**
     * Get the original reference.
     * 
     * @return A reference
     */
    public Reference getReference() {
        return reference;
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