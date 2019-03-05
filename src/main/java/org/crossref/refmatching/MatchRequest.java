package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;

/**
 * This contains user-specified inputs for executing matching logic.
 * 
 * @author joe.aparo
 */
public class MatchRequest {
    public static final double DEFAULT_CAND_MIN_SCORE = 0.4;
    public static final double DEFAULT_UNSTR_MIN_SCORE = 0.34;
    public static final double DEFAULT_STR_MIN_SCORE = 0.76;
    public static final int DEFAULT_STR_ROWS = 100;
    public static final int DEFAULT_UNSTR_ROWS = 20;
    private static final String DEFAULT_DELIMITER = "\r?\n";

    private final InputType inputType;
    private final String inputValue;
    private double candidateMinScore = DEFAULT_CAND_MIN_SCORE;
    private double unstructuredMinScore = DEFAULT_UNSTR_MIN_SCORE;
    private double structuredMinScore = DEFAULT_STR_MIN_SCORE;
    private String dataDelimiter = DEFAULT_DELIMITER;
    private int unstructuredRows = DEFAULT_UNSTR_ROWS;
    private int structuredRows = DEFAULT_STR_ROWS;
    private String mailTo = null;
    private Map<String, String> headers = new HashMap<String, String>();
    
    public MatchRequest(InputType inputType, String inputValue) {
        this.inputType = inputType;
        this.inputValue = inputValue;
    }

    public MatchRequest(
        InputType inputType, String inputValue, double candidateMinScore, 
        double unstructuredMinScore, double structuredMinScore,
        int unstructuredRows, int structuredRows) {
        
        this.inputType = inputType;
        this.candidateMinScore = candidateMinScore;
        this.unstructuredMinScore = unstructuredMinScore;
        this.structuredMinScore = structuredMinScore;
        this.inputValue = inputValue;
        this.unstructuredRows = unstructuredRows;
        this.structuredRows = structuredRows;
    }

    public InputType getInputType() {
        return inputType;
    }

     public String getInputValue() {
        return inputValue;
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

    public String getDataDelimiter() {
        return dataDelimiter;
    }

    public void setDataDelimiter(String dataDelimiter) {
        this.dataDelimiter = dataDelimiter;
    }

    public int getUnstructuredRows() {
        return unstructuredRows;
    }

    public void setUnstructuredRows(int unstructuredRows) {
        this.unstructuredRows = unstructuredRows;
    }

    public int getStructuredRows() {
        return structuredRows;
    }

    public void setStructuredRows(int structuredRows) {
        this.structuredRows = structuredRows;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }
    
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(this.headers);
    }
}

