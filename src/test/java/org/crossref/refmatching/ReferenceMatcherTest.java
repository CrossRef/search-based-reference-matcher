package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.ResourceUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author joe.aparo
 */
public class ReferenceMatcherTest {
    private static final double STRING_VALID_TH = 0.34;
    private static final double STRUCTURED_VALID_TH = 0.65;
    
    private final MatchRequest request = new MatchRequest();
    private final Map<String, String> mockResponseMap = new HashMap<>();
    
    @Mock
    ICrossRefApiClient apiTestClient;
    
    ReferenceMatcher matcher;
    
    @Before
    public void setupMock() {
        // Cache mock API responses
        loadMockResponseMap();
        
        MockitoAnnotations.initMocks(this);
        
        matcher = new ReferenceMatcher(apiTestClient);
        matcher.setCacheJournals(true);
        matcher.initialize();
    }
    
    @Test
    public void shouldMatch_whenStructuredRefIsFound() {
        JSONObject reference = new JSONObject();
        reference.put("author", "Tkaczyk");
        reference.put("volume", "18");
        reference.put("first-page", "317");
        reference.put("year", "2015");
        reference.put("journal-title", "IJDAR");
        
        MatchResponse response = invokeMockStringRequest(reference, "structured-ref-response-1.json");
        
        Assert.assertTrue(response.getMatches().size() == 1);
        Assert.assertTrue(response.getMatches().get(0).getDOI().equals("10.1007/s10032-015-0249-8"));
    }
    
    @Test
    public void shouldNotMatch_whenNoStructuredRefIsNotFound() {
        JSONObject reference = new JSONObject();
        reference = new JSONObject();
        reference.put("author", "Tkaczyk");
        reference.put("volume", "93");
        reference.put("year", "2015");
        reference.put("journal-title", "Communications in Computer and Information");

        MatchResponse response = invokeMockStringRequest(reference, "structured-ref-response-2.json");
        
        Assert.assertTrue(response.getMatches().size() == 1);
        Assert.assertTrue(response.getMatches().get(0).getDOI() == null);
    }
    
    @Test
    public void shouldMatch_whenUntructuredRefIsFound() {
        
        String reference = 
            "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and "
            + "Bolikowski,“CERMINE: automatic extraction of structured "
            + "metadata from scientific literature,” International Journal "
            + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
            + "pp. 317–335, 2015.";
        
        MatchResponse response = 
            invokeMockStringRequest(reference, "unstructured-ref-response-1.json");
        
        Assert.assertTrue(response.getMatches().size() == 1);
        Assert.assertTrue(response.getMatches().get(0).getDOI().equals("10.1007/s10032-015-0249-8"));
    }
    
    @Test
    public void shouldNotMatch_whenUntructuredRefIsNotFound() {
        
        String reference = 
            "P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. Bolikowski,"
            + "“CERMINE: automatic extraction of structured metadata from "
            + "scientific literature,” IJDAR, vol. 14, no. 40, pp. 310–335, "
            + "2016.";
        
        MatchResponse response = 
            invokeMockStringRequest(reference, "unstructured-ref-response-2.json");
        
        Assert.assertTrue(response.getMatches().size() == 1);
        Assert.assertTrue(response.getMatches().get(0).getDOI() == null);
    }

    public void candidatePropertiesShouldMatch_whenComparedToThoseGiven() {
        JSONObject item = new JSONObject(mockResponseMap.get("single-doi-response-1.json"));       
        Candidate candidate = new Candidate(item);
        
        candidate.setValidationScore(0.8);

        Assert.assertEquals(item, candidate.getItem());
        Assert.assertEquals("10.1007/s10032-015-0249-8", candidate.getDOI());
        Assert.assertEquals(0.8, candidate.getValidationScore(), 0.0001);
    }
    
    public void similarityShouldCorrespondToScore_whenUnstructuredRefsGiven() {
        JSONObject item = new JSONObject(mockResponseMap.get("single-doi-response-1.json"));       
        item.put("score", 50);
        Candidate candidate = new Candidate(item);

        Assert.assertTrue(candidate.getStringValidationSimilarity(
            "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. "
            + "Bolikowski, “CERMINE: automatic extraction of structured "
            + "metadata from scientific literature,” International Journal "
            + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
            + "pp. 317–335, 2015.") > STRING_VALID_TH);
        Assert.assertTrue(candidate.getStringValidationSimilarity(
            "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
            + "Bolikowski, International Journal on Document Analysis and "
            + "Recognition (IJDAR) 18 (2015) 317.") > STRING_VALID_TH);
        Assert.assertTrue(candidate.getStringValidationSimilarity(
            "[1]D. Tkaczyk and Ł. Bolikowski, “Extracting Contextual "
            + "Information from Scientific Literature Using CERMINE System,” "
            + "Communications in Computer and Information Science, pp. "
            + "93–104, 2015.") < STRING_VALID_TH);
        Assert.assertTrue(candidate.getStringValidationSimilarity(
            "[1] D. Tkaczyk, Ł. Bolikowski, Communications in Computer and "
            + "Information Science (2015) 93.") < STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(
            "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. "
            + "Bolikowski,“CERMINE: automatic extraction of structured "
            + "metadata from scientific literature,” International Journal "
            + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
            + "pp. 317–335, 2015.") > STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(
            "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
            + "Bolikowski, International Journal on Document Analysis and "
            + "Recognition (IJDAR) 18 (2015) 317.") > STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(
            "[1]D. Tkaczyk and Ł. Bolikowski, “Extracting Contextual "
            + "Information from Scientific Literature Using CERMINE System,” "
            + "Communications in Computer and Information Science, pp. "
            + "93–104, 2015.") < STRING_VALID_TH);
        Assert.assertTrue(candidate.getValidationSimilarity(
            "[1] D. Tkaczyk, Ł. Bolikowski, Communications in Computer and "
            + "Information Science (2015) 93.") < STRING_VALID_TH);
    }
    
    public void similarityShouldCorrespondToScore_whenStructuredRefsGiven() {
        JSONObject item = new JSONObject(mockResponseMap.get("single-doi-response-1.json"));       
        item.put("score", 50);
        Candidate candidate = new Candidate(item);

        Map<String, String> fields = new HashMap<>();
        fields.put("author", "Tkaczyk");
        fields.put("volume", "18");
        fields.put("first-page", "317");
        fields.put("year", "2015");
        fields.put("journal-title", "IJDAR");
        StructuredReference reference = new StructuredReference(fields);

        Assert.assertTrue(
            candidate.getStructuredValidationSimilarity(reference) > STRUCTURED_VALID_TH);
        Assert.assertTrue(
            candidate.getValidationSimilarity(reference) > STRUCTURED_VALID_TH);

        fields = new HashMap<>();
        fields.put("author", "Tkaczyk");
        fields.put("volume", "93");
        fields.put("year", "2015");
        fields.put("journal-title", "Communications in Computer and Information");
        reference = new StructuredReference(fields);

        Assert.assertTrue(
            candidate.getStructuredValidationSimilarity(reference) < STRUCTURED_VALID_TH);
        Assert.assertTrue(
            candidate.getValidationSimilarity(reference) < STRUCTURED_VALID_TH);
    }
    
    private MatchResponse invokeMockStringRequest(Object reference, String mockJsonFileNane) {
         try {
            when(apiTestClient.getWorks(any())).thenReturn(mockResponseMap.get(mockJsonFileNane));
            MatchRequest request = new MatchRequest(RequestInputType.STRING, null, reference.toString());
 
            return matcher.match(request);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void loadMockResponseMap() {
        File[] files = ResourceUtils.getResourceFolderFiles("api-responses");
        for (File f : files) {
            try {
                mockResponseMap.put(f.getName(), FileUtils.readFileToString(f, "UTF-8"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}