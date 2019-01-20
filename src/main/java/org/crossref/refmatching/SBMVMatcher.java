package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
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
    private static final double DEFAULT_STR_MIN_SCORE = 0.76;

    private CandidateSelector selector;
    private CandidateValidator validatorUnstructured;
    private CandidateValidator validatorStructured;

    protected Map<String, String> journals = new HashMap<>();

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

        try {
            URL url = SBMVMatcher.class.getResource("journal-abbreviations.txt");
            File file = new File(url.toURI());
            List<String> lines = FileUtils.readLines(file, "utf-8");
            lines.stream()
                    .map(l -> l.trim().split("\t"))
                    .forEach(a -> {journals.put(a[0], a[1]);});
        } catch (URISyntaxException | IOException ex) {
            LOGGER.warn(ex);
        }
    }

    public Candidate match(String refString) {
        LOGGER.debug(String.format("Matching reference: %s", refString));
        Candidate candidate = match(new UnstructuredReference(refString),
                validatorUnstructured);
        LOGGER.debug(String.format("Reference %s matched to %s", refString,
                (candidate == null) ? "null" : "DOI " + candidate.getDOI()));
        return candidate;
    }

    public Candidate match(JSONObject reference) {
        LOGGER.debug(String.format("Matching reference: %s", reference));
        Candidate candidate = match(new StructuredReference(reference),
                validatorStructured);
        String journalNorm = reference.optString("journal-title").toLowerCase().replaceAll("[^a-z]", "");
        if (journals.containsKey(journalNorm)) {
            reference.put("journal-title", journals.get(journalNorm));
            Candidate candidate2 = match(new StructuredReference(reference),
                    validatorStructured);
            if (candidate == null) {
                return candidate2;
            }
            if (candidate2 == null) {
                return candidate;
            }
            if (candidate2.getValidationScore() > candidate.getValidationScore()) {
                return candidate2;
            }
        }
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
        List<Candidate> candidates = selector.findCandidates(reference);
        return validator.chooseCandidate(reference, candidates);
    }

}
