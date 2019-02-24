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
    public static final int STR_ROWS = 100;
    public static final int UNSTR_ROWS = 20;
    
    private static final String DEFAULT_DELIMITER = "\r?\n";

    private InputType inputType = null;
    private double candidateMinScore = DEFAULT_CAND_MIN_SCORE;
    private double unstructuredMinScore = DEFAULT_UNSTR_MIN_SCORE;
    private double structuredMinScore = DEFAULT_STR_MIN_SCORE;
    private String inputValue;
    private String dataDelimiter = DEFAULT_DELIMITER;
    
    public MatchRequest(InputType inputType, String inputValue) {
        this.inputType = inputType;
        this.inputValue = inputValue;
    }

    public MatchRequest(
        InputType inputType, String inputValue, double candidateMinScore, 
        double unstructuredMinScore, double structuredMinScore) {
        
        this.inputType = inputType;
        this.candidateMinScore = candidateMinScore;
        this.unstructuredMinScore = unstructuredMinScore;
        this.structuredMinScore = structuredMinScore;
        this.inputValue = inputValue;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
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

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public String getDataDelimiter() {
        return dataDelimiter;
    }

    public void setDataDelimiter(String dataDelimiter) {
        this.dataDelimiter = dataDelimiter;
    }
}
