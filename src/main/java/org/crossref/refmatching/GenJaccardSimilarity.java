package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;

/**
 * Generalized Jaccard similarity.
 * 
 * @author Dominika Tkaczyk
 */
public class GenJaccardSimilarity {

    private final Map<String, Double> first = new HashMap();
    private final Map<String, Double> second = new HashMap();

    public Double getMinWeight(String key) {
        if (!first.containsKey(key)) {
            return null;
        }
        return Math.min(first.get(key), second.get(key));
    }

    public void update(String key, double weight1, double weight2) {
        first.put(key, weight1);
        second.put(key, weight2);
    }

    public double similarity() {
        Double numerator = first.keySet().stream()
                .map(k -> Math.min(first.get(k), second.get(k)))
                .reduce(0., (a, b) -> a + b);
        Double denominator = first.keySet().stream()
                .map(k -> Math.max(first.get(k), second.get(k)))
                .reduce(0., (a, b) -> a + b);
        return (denominator == 0) ? 1. : numerator / denominator;
    }

    @Override
    public String toString() {
        return "GenJaccardSimilarity{" + "first=" + first +
                ", second=" + second + '}';
    }

}