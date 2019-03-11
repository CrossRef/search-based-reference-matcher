package org.crossref.refmatching;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class for selecting a single target document from a list of candidates.
 * 
 * @author Dominika Tkaczyk
 */
public class CandidateValidator {

    public Candidate chooseCandidate(Reference reference,
            List<Candidate> candidates, double minScore) {
        if (candidates.isEmpty()) {
            return null;
        }
        
        List<Double> scores = candidates.stream()
                .map(c -> c.getValidationSimilarity(reference))
                .collect(Collectors.toList());
        
        int bestIndex = IntStream.range(0, scores.size())
                .reduce(0, (a, b) -> (scores.get(a) >= scores.get(b)) ? a : b);
        
        candidates.get(bestIndex).setValidationScore(scores.get(bestIndex));
        
        return (scores.get(bestIndex) >= minScore)
                ? candidates.get(bestIndex) : null;
    }

}