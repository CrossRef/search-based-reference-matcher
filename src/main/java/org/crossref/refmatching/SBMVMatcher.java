package org.crossref.refmatching;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 *
 * @author Dominika Tkaczyk
 */
public class SBMVMatcher {

    private static final double DEFAULT_CAND_MIN_SCORE = 0.4;
    private static final double DEFAULT_UNSTR_MIN_SCORE = 0.34;
    private static final double DEFAULT_STR_MIN_SCORE = 0.67;

    private CandidateSelector selector;
    private CandidateValidator validatorUnstructured;
    private CandidateValidator validatorStructured;
    
    private static final Logger LOGGER =
            LogManager.getLogger(SBMVMatcher.class.getName());

    public SBMVMatcher() {
        this(null, null, null);
    }
    
    public SBMVMatcher(Double candidateMinScore, Double unstructuredMinScore,
            Double structuredMinScore) {
        if (candidateMinScore == null) {
            candidateMinScore = DEFAULT_CAND_MIN_SCORE;
        }
        if (unstructuredMinScore == null) {
            unstructuredMinScore = DEFAULT_UNSTR_MIN_SCORE;
        }
        if (structuredMinScore == null) {
            structuredMinScore = DEFAULT_STR_MIN_SCORE;
        }
        selector = new CandidateSelector(candidateMinScore);
        validatorUnstructured = new CandidateValidator(unstructuredMinScore);
        validatorStructured = new CandidateValidator(structuredMinScore);
    }

    public Candidate match(String refString) {
        LOGGER.debug(String.format("Matching reference: %s", refString));
        Candidate candidate =  match(new UnstructuredReference(refString),
                validatorUnstructured);
        LOGGER.debug(String.format("Reference %s matched to %s", refString,
                (candidate == null) ? "null" : "DOI " + candidate.getDOI()));
        return candidate;
    }

    public Candidate match(JSONObject reference) {
        LOGGER.debug(String.format("Matching reference: %s", reference));
        Candidate candidate = match(new StructuredReference(reference),
                validatorStructured);
        LOGGER.debug(String.format("Reference %s matched to %s", reference,
                (candidate == null) ? "null" : "DOI " + candidate.getDOI()));
        return candidate;
    }
    
    private Candidate match(Reference reference, CandidateValidator validator) {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException ex) {
            LOGGER.error("Error waiting", ex);
        }
        var candidates = selector.findCandidates(reference);
        return validator.chooseCandidate(reference, candidates);
    }
    
}