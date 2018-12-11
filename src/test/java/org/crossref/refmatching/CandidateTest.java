package org.crossref.refmatching;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class CandidateTest {

    public static double STRING_VALID_TH = 0.34;
    public static double STRUCTURED_VALID_TH = 0.65;

    private JSONObject retrieve(String doi) throws IOException {
        URL url = new URL("https://api.crossref.org/works/" + doi);
        JSONObject json = new JSONObject(IOUtils.toString(url, "UTF-8"));
        return json.getJSONObject("message");
    }

    @Test
    public void testBasic() throws IOException {
        JSONObject item = retrieve("10.1007/s10032-015-0249-8");
        Candidate candidate = new Candidate(item);
        candidate.setValidationScore(0.8);

        assertEquals(item, candidate.getItem());
        assertEquals("10.1007/s10032-015-0249-8", candidate.getDOI());
        assertEquals(0.8, candidate.getValidationScore(), 0.0001);
    }

    @Test
    public void testStringValidationSimilarity() throws IOException {
        JSONObject item = retrieve("10.1007/s10032-015-0249-8");
        item.put("score", 50);
        Candidate candidate = new Candidate(item);

        assertTrue(candidate.getStringValidationSimilarity(
                "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. "
                + "Bolikowski, “CERMINE: automatic extraction of structured "
                + "metadata from scientific literature,” International Journal "
                + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
                + "pp. 317–335, 2015.") > STRING_VALID_TH);
        assertTrue(candidate.getStringValidationSimilarity(
                "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
                + "Bolikowski, International Journal on Document Analysis and "
                + "Recognition (IJDAR) 18 (2015) 317.") > STRING_VALID_TH);
        assertTrue(candidate.getStringValidationSimilarity(
                "[1]D. Tkaczyk and Ł. Bolikowski, “Extracting Contextual "
                + "Information from Scientific Literature Using CERMINE System,” "
                + "Communications in Computer and Information Science, pp. "
                + "93–104, 2015.") < STRING_VALID_TH);
        assertTrue(candidate.getStringValidationSimilarity(
                "[1] D. Tkaczyk, Ł. Bolikowski, Communications in Computer and "
                + "Information Science (2015) 93.") < STRING_VALID_TH);

        assertTrue(candidate.getValidationSimilarity(new UnstructuredReference(
                "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. "
                + "Bolikowski,“CERMINE: automatic extraction of structured "
                + "metadata from scientific literature,” International Journal "
                + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
                + "pp. 317–335, 2015.")) > STRING_VALID_TH);
        assertTrue(candidate.getValidationSimilarity(new UnstructuredReference(
                "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
                + "Bolikowski, International Journal on Document Analysis and "
                + "Recognition (IJDAR) 18 (2015) 317.")) > STRING_VALID_TH);
        assertTrue(candidate.getValidationSimilarity(new UnstructuredReference(
                "[1]D. Tkaczyk and Ł. Bolikowski, “Extracting Contextual "
                + "Information from Scientific Literature Using CERMINE System,” "
                + "Communications in Computer and Information Science, pp. "
                + "93–104, 2015.")) < STRING_VALID_TH);
        assertTrue(candidate.getValidationSimilarity(new UnstructuredReference(
                "[1] D. Tkaczyk, Ł. Bolikowski, Communications in Computer and "
                + "Information Science (2015) 93.")) < STRING_VALID_TH);
    }

    @Test
    public void testStructuredValidationSimilarity() throws IOException {
        JSONObject item = retrieve("10.1007/s10032-015-0249-8");
        item.put("score", 50);
        Candidate candidate = new Candidate(item);

        Map<String, String> fields = new HashMap<>();
        fields.put("author", "Tkaczyk");
        fields.put("volume", "18");
        fields.put("first-page", "317");
        fields.put("year", "2015");
        fields.put("journal-title", "IJDAR");
        StructuredReference reference = new StructuredReference(fields);

        assertTrue(candidate.getStructuredValidationSimilarity(reference)
                > STRUCTURED_VALID_TH);
        assertTrue(candidate.getValidationSimilarity(reference)
                > STRUCTURED_VALID_TH);

        fields = new HashMap<>();
        fields.put("author", "Tkaczyk");
        fields.put("volume", "93");
        fields.put("year", "2015");
        fields.put("journal-title", "Communications in Computer and Information");
        reference = new StructuredReference(fields);

        assertTrue(candidate.getStructuredValidationSimilarity(reference)
                < STRUCTURED_VALID_TH);
        assertTrue(candidate.getValidationSimilarity(reference)
                < STRUCTURED_VALID_TH);
    }

}
