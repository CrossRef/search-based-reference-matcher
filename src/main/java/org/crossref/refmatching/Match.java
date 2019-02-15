/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.crossref.refmatching;

/**
 * This represents a matched reference.
 * 
 * @author joe.aparo
 */
public class Match {
    private Object reference;
    private String doi;
    private double score;

    public Match(Object reference, String doi, double score) {
        this.reference = reference;
        this.doi = doi;
        this.score = score;
    }

    /**
     * Get the original reference that the item matched on.
     * @return A String, or a JSONObject
     */
    public Object getReference() {
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
