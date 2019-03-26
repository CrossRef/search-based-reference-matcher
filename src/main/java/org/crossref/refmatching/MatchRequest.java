 package org.crossref.refmatching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains user-specified inputs for executing matching logic.
 * 
 * @author Joe Aparo
 */
public class MatchRequest {
    public static final double DEFAULT_CAND_MIN_SCORE = 0.4;
    public static final double DEFAULT_UNSTR_MIN_SCORE = 0.34;
    public static final double DEFAULT_STR_MIN_SCORE = 0.76;
    public static final int DEFAULT_STR_ROWS = 100;
    public static final int DEFAULT_UNSTR_ROWS = 20;
    public static final int DEFAULT_NUM_THREADS = 4;
    public static final int MAX_THREADS = 30;

    private double candidateMinScore = DEFAULT_CAND_MIN_SCORE;
    private double unstructuredMinScore = DEFAULT_UNSTR_MIN_SCORE;
    private double structuredMinScore = DEFAULT_STR_MIN_SCORE;
    private int unstructuredRows = DEFAULT_UNSTR_ROWS;
    private int structuredRows = DEFAULT_STR_ROWS;
    private int numThreads = DEFAULT_NUM_THREADS;
    private final Map<String, String> headers = new HashMap<String, String>();
    private final List<ReferenceData> references;

    public MatchRequest(List<ReferenceData> references) {
        if (references == null) {
            references = new ArrayList<>();
        }
        this.references = references;
    }

    public MatchRequest(List<ReferenceData> references,
            double candidateMinScore, double unstructuredMinScore,
            double structuredMinScore, int unstructuredRows, int structuredRows) {
        this.references = references;
        this.candidateMinScore = candidateMinScore;
        this.unstructuredMinScore = unstructuredMinScore;
        this.structuredMinScore = structuredMinScore;
        this.unstructuredRows = unstructuredRows;
        this.structuredRows = structuredRows;
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

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = Math.min(Math.min(Math.max(1, numThreads), 
            MatchRequest.MAX_THREADS), references.size());
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
     * Get the list of queries associated with the request.
     * @return A list of query objects
     */
    public List<ReferenceData> getReferences() {
        return references.subList(0, references.size());
    }
}