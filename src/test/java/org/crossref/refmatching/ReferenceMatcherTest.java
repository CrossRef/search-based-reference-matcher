package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.apache.commons.io.FileUtils;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.ResourceUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class ReferenceMatcherTest {
    private final Map<String, String> mockResponseMap = new HashMap<>();
    
    @Mock
    ICrossRefApiClient apiTestClient;
    
    ReferenceMatcher matcher;
    
    @Mock
    ReferenceMatcher matcherMock;
    
    @Before
    public void setupMock() {
        // Cache mock API responses
        loadMockResponseMap();
        
        MockitoAnnotations.initMocks(this);
        
        matcher = new ReferenceMatcher(apiTestClient);
        matcher.setCacheJournalAbbrevMap(true);
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
        
        MatchResponse response = invokeMockStringRequest(reference.toString(),
                "structured-ref-response-1.json");
        
        Assert.assertEquals(1, response.getMatchedLinks().size());
        Assert.assertEquals("10.1007/s10032-015-0249-8",
                response.getMatchedLinks().get(0).getDOI());
    }
    
    @Test
    public void shouldNotMatch_whenNoStructuredRefIsNotFound() {
        JSONObject reference = new JSONObject();
        reference.put("author", "Tkaczyk");
        reference.put("volume", "93");
        reference.put("year", "2015");
        reference.put("journal-title",
                "Communications in Computer and Information");

        MatchResponse response = invokeMockStringRequest(reference.toString(),
                "structured-ref-response-2.json");
        
        Assert.assertEquals(1, response.getMatchedLinks().size());
        Assert.assertNull(response.getMatchedLinks().get(0).getDOI());
    }
    
    @Test
    public void shouldMatch_whenUnstructuredRefIsFound() {
        
        String reference = 
            "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and "
            + "Bolikowski,“CERMINE: automatic extraction of structured "
            + "metadata from scientific literature,” International Journal "
            + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
            + "pp. 317–335, 2015.";
        
        MatchResponse response = invokeMockStringRequest(reference,
                "unstructured-ref-response-1.json");
        
        Assert.assertEquals(1, response.getMatchedLinks().size());
        Assert.assertEquals("10.1007/s10032-015-0249-8",
                response.getMatchedLinks().get(0).getDOI());
    }
    
    @Test
    public void shouldNotMatch_whenUnstructuredRefIsNotFound() {
        
        String reference = 
            "P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. Bolikowski,"
            + "“CERMINE: automatic extraction of structured metadata from "
            + "scientific literature,” IJDAR, vol. 14, no. 40, pp. 310–335, "
            + "2016.";
        
        MatchResponse response = invokeMockStringRequest(reference,
                "unstructured-ref-response-2.json");
        
        Assert.assertEquals(1, response.getMatchedLinks().size());
        Assert.assertNull(response.getMatchedLinks().get(0).getDOI());
    }
    
    @Test
    public void shouldPreserveReferenceOrder() {
        String references = "ref1\nref2\n{\"ref\":\"3\"}\nref4\nref5";
        
        MatchResponse response = invokeMockStringRequest(references,
                "unstructured-ref-response-1.json");
        
        Assert.assertEquals(5, response.getMatchedLinks().size());
        Assert.assertEquals("ref1",
                response.getMatchedLinks().get(0).getReferenceData()
                .getReference().getFormattedString());
        Assert.assertEquals("ref2",
                response.getMatchedLinks().get(1).getReferenceData()
                .getReference().getFormattedString());
        Assert.assertEquals("{\"ref\":\"3\"}",
                response.getMatchedLinks().get(2).getReferenceData()
                .getReference().getMetadataAsJSON().toString());
        Assert.assertEquals("ref4",
                response.getMatchedLinks().get(3).getReferenceData()
                .getReference().getFormattedString());
        Assert.assertEquals("ref5",
                response.getMatchedLinks().get(4).getReferenceData()
                .getReference().getFormattedString());
    }
    
    @Test(timeout = 60000)
    public void shouldRunInParallel() throws IOException, InterruptedException {
        Semaphore reportSem = new Semaphore(0);
        Semaphore ackSem = new Semaphore(0);

        Thread t = new Thread() {
            public void run() {
                try {
                    String references = "ref1\n{\"ref\":\"2\"}\nref3";
                    MatchRequest request = new MatchRequest(
                            Utils.parseInputReferences(InputType.STRING,
                                    references, "\r?\n"));

                    when(matcherMock.match(eq(request))).thenCallRealMethod();
                    Answer<ReferenceLink> answer =
                            (InvocationOnMock invocation) -> {
                        reportSem.release();
                        ackSem.acquire();
                        return null;
                    };
                    when(matcherMock.matchUnstructured(any(ReferenceData.class),
                            eq(request)))
                            .thenAnswer(answer);
                    when(matcherMock.matchStructured(any(ReferenceData.class),
                            eq(request)))
                            .thenAnswer(answer);
                    matcherMock.match(request);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

        };
        t.start();

        reportSem.acquire(3);
        ackSem.release(3);
    }
    
    private MatchResponse invokeMockStringRequest(String reference,
            String mockJsonFileName) {
         try {
            when(apiTestClient.getWorks(any(), any()))
                    .thenReturn(extractMockItems(mockJsonFileName));
            
            MatchRequest request = new MatchRequest(
                    Utils.parseInputReferences(InputType.STRING,
                            reference, "\r?\n"));
            
            return matcher.match(request);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private JSONArray extractMockItems(String mockJsonFileName) {
        JSONObject json = new JSONObject(mockResponseMap.get(mockJsonFileName));
        return json.getJSONObject("message").optJSONArray("items");        
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