package org.crossref.refmatching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public static final String DEFAULT_DELIMITER = "\r?\n";
    
    private InputType inputType;
    private String inputValue;
    private double candidateMinScore = DEFAULT_CAND_MIN_SCORE;
    private double unstructuredMinScore = DEFAULT_UNSTR_MIN_SCORE;
    private double structuredMinScore = DEFAULT_STR_MIN_SCORE;
    private String dataDelimiter = DEFAULT_DELIMITER;
    private int unstructuredRows = DEFAULT_UNSTR_ROWS;
    private int structuredRows = DEFAULT_STR_ROWS;
    private final Map<String, String> headers = new HashMap<>();
    private final List<ReferenceQuery> queries = new ArrayList<>();
    
    /**
     * Used when specifying queries directly in the request only.
     */
    public MatchRequest() {
    }
    
    /**
     * Used when including queries parsed from textual input data.
     * 
     * @param inputType The source of the textual data.
     * @param inputValue The value corresponding to input type.
     */
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

   
    /**
     * Ad a header to be passed via the CR-API http client
     * @param key
     * @param value 
     */
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }
    
    /**
     * Set headers to be passed via the CR-API http client.
     * @param headers Key/value pairs
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    /**
     * Get the headers to be passed via the CR-API client.
     * @return A list of key/value pairs
     */
    public Map<String, String> getHeaders() {
        return new HashMap<>(this.headers);
    }
    
    /**
     * Add a query object to the request.
     * @param query A query object
     */
    public void addQuery(ReferenceQuery query) {
        queries.add(query);
    }
    
    /**
     * Get the list of queries associated with the request.
     * @return A list of query objects
     */
    public List<ReferenceQuery> getQueries() {
        return queries.subList(0, queries.size());
    }
}

