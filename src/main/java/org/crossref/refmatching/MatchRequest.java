/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.crossref.refmatching;

/**
 * This contains user-specified inputs for executing matching logic.
 * 
 * @author joe.aparo
 */
public class MatchRequest {
    public static final double DEFAULT_CAND_MIN_SCORE = 0.4;
    public static final double DEFAULT_UNSTR_MIN_SCORE = 0.34;
    public static final double DEFAULT_STR_MIN_SCORE = 0.76;
    
    private RequestInputType inputType = null;
    private double candidateMinScore = DEFAULT_CAND_MIN_SCORE;
    private double unstructuredMinScore = DEFAULT_UNSTR_MIN_SCORE;
    private double structuredMinScore = DEFAULT_STR_MIN_SCORE;
    private String inputFileName;
    private String refString;
    
    public MatchRequest() {}
    
    public MatchRequest(
        RequestInputType inputType,
        String inputFileName, String refString) {
        
        this.inputType = inputType;
        this.inputFileName = inputFileName;
        this.refString = refString;
    }

    public MatchRequest(
        RequestInputType inputType, double candidateMinScore, 
        double unstructuredMinScore, double structuredMinScore,
        String inputFileName, String refString) {
        
        this.inputType = inputType;
        this.candidateMinScore = candidateMinScore;
        this.unstructuredMinScore = unstructuredMinScore;
        this.structuredMinScore = structuredMinScore;
        this.inputFileName = inputFileName;
        this.refString = refString;
    }

    public RequestInputType getInputType() {
        return inputType;
    }

    public void setInputType(RequestInputType inputType) {
        this.inputType = inputType;
    }

    public double getCandidateMinScore() {
        return candidateMinScore;
    }

    public void setCandidateMinScore(double candidateMinScore) {
        this.candidateMinScore = candidateMinScore;
    }

    public double getUnstructuredMinScore() {
        return unstructuredMinScore;
    }

    public void setUnstructuredMinScore(double unstructuredMinScore) {
        this.unstructuredMinScore = unstructuredMinScore;
    }

    public double getStructuredMinScore() {
        return structuredMinScore;
    }

    public void setStructuredMinScore(double structuredMinScore) {
        this.structuredMinScore = structuredMinScore;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    public String getRefString() {
        return refString;
    }

    public void setRefString(String refString) {
        this.refString = refString;
    }
}
