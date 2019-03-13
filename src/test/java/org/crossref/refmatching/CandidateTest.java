package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.crossref.common.utils.ResourceUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class CandidateTest {
    private static final double STRING_VALID_TH = 0.34;
    private static final double STRUCTURED_VALID_TH = 0.65;
    
    private final Map<String, String> mockResponseMap = new HashMap<>();
    
    @Before
    public void setupMock() {
        // Cache mock API responses
        loadMockResponseMap();
        
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void candidatePropertiesShouldMatch_whenComparedToThoseGiven() {
        JSONObject item = extractFirstMockItem("single-doi-response-1.json");
        Candidate candidate = new Candidate(item);
        
        candidate.setValidationScore(0.8);

        Assert.assertEquals(item, candidate.getItem());
        Assert.assertEquals("10.1007/s10032-015-0249-8", candidate.getDOI());
        Assert.assertEquals(0.8, candidate.getValidationScore(), 0.0001);
    }
    
    @Test
    public void similarityShouldCorrespondToScore_whenUnstructuredRefsGiven() {   
        JSONObject item = extractFirstMockItem("single-doi-response-1.json");
        item.put("score", 50);
        Candidate candidate = new Candidate(item);

        Assert.assertTrue(candidate.getStringValidationSimilarity(new Reference(
            "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. "
            + "Bolikowski, “CERMINE: automatic extraction of structured "
            + "metadata from scientific literature,” International Journal "
            + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
            + "pp. 317–335, 2015.")) > STRING_VALID_TH);
        Assert.assertTrue(candidate.getStringValidationSimilarity(new Reference(
            "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
            + "Bolikowski, International Journal on Document Analysis and "
            + "Recognition (IJDAR) 18 (2015) 317.")) > STRING_VALID_TH);
        Assert.assertTrue(candidate.getStringValidationSimilarity(new Reference(
            "[1]D. Tkaczyk and Ł. Bolikowski, “Extracting Contextual "
            + "Information from Scientific Literature Using CERMINE System,” "
            + "Communications in Computer and Information Science, pp. "
            + "93–104, 2015.")) < STRING_VALID_TH);
        Assert.assertTrue(candidate.getStringValidationSimilarity(new Reference(
            "[1] D. Tkaczyk, Ł. Bolikowski, Communications in Computer and "
            + "Information Science (2015) 93.")) < STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(new Reference(
            "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. "
            + "Bolikowski,“CERMINE: automatic extraction of structured "
            + "metadata from scientific literature,” International Journal "
            + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
            + "pp. 317–335, 2015.")) > STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(new Reference(
            "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
            + "Bolikowski, International Journal on Document Analysis and "
            + "Recognition (IJDAR) 18 (2015) 317.")) > STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(new Reference(
            "[1]D. Tkaczyk and Ł. Bolikowski, “Extracting Contextual "
            + "Information from Scientific Literature Using CERMINE System,” "
            + "Communications in Computer and Information Science, pp. "
            + "93–104, 2015.")) < STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(new Reference(
            "[1] D. Tkaczyk, Ł. Bolikowski, Communications in Computer and "
            + "Information Science (2015) 93.")) < STRING_VALID_TH);
    }
    
    @Test
    public void similarityShouldCorrespondToScore_whenStructuredRefsGiven() {
        JSONObject item = extractFirstMockItem("single-doi-response-1.json");    
        item.put("score", 50);
        Candidate candidate = new Candidate(item);

        Map<String, String> fields = new HashMap<>();
        fields.put("author", "Tkaczyk");
        fields.put("volume", "18");
        fields.put("first-page", "317");
        fields.put("year", "2015");
        fields.put("journal-title", "IJDAR");
        Reference reference = new Reference(fields);

        Assert.assertTrue(
            candidate.getStructuredValidationSimilarity(reference)
                    > STRUCTURED_VALID_TH);
        Assert.assertTrue(
            candidate.getValidationSimilarity(reference) > STRUCTURED_VALID_TH);

        fields = new HashMap<>();
        fields.put("author", "Tkaczyk");
        fields.put("volume", "93");
        fields.put("year", "2015");
        fields.put("journal-title", "Communications in Computer and Information");
        reference = new Reference(fields);

        Assert.assertTrue(
            candidate.getStructuredValidationSimilarity(reference)
                    < STRUCTURED_VALID_TH);
        Assert.assertTrue(
            candidate.getValidationSimilarity(reference) < STRUCTURED_VALID_TH);
    }
    
    private JSONArray extractMockItems(String mockJsonFileName) {
        JSONObject json = new JSONObject(mockResponseMap.get(mockJsonFileName));
        return json.getJSONObject("message").optJSONArray("items");        
    }
    
    private JSONObject extractFirstMockItem(String mockJsonFileName) {
        JSONArray items = extractMockItems(mockJsonFileName);
        return items.getJSONObject(0);
    }
    
    private void loadMockResponseMap() {
        File[] files = ResourceUtils.getResourceFolderFiles("api-responses");
        for (File f : files) {
            try {
                mockResponseMap.put(f.getName(),
                        FileUtils.readFileToString(f, "UTF-8"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}