package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.ResourceUtils;
import org.json.JSONArray;
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
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class ReferenceMatcherTest {
    private final Map<String, String> mockResponseMap = new HashMap<>();
    
    @Mock
    ICrossRefApiClient apiTestClient;
    
    ReferenceMatcher matcher;
    
    List<String> sampleRefsUnstructured;
    List<Reference> sampleRefsStructured;
    
    
    @Before
    public void setupMock() {
        // Cache mock API responses
        loadMockResponseMap();
        
        MockitoAnnotations.initMocks(this);
        
        matcher = new ReferenceMatcher(apiTestClient);
        matcher.setCacheJournalAbbrevMap(true);
        matcher.initialize();
        
        sampleRefsUnstructured = ResourceUtils.readResourceAsLines(
                "/test-inputs/sample-ref-strings-2000.txt");
        JSONArray refArray = new JSONArray(ResourceUtils.readResourceAsString(
                "/test-inputs/sample-refs-2000.json"));
        sampleRefsStructured = IntStream.range(0, refArray.length())
                .mapToObj(i -> refArray.get(i) instanceof String ?
                        new Reference(refArray.getString(i)) :
                        new Reference(refArray.getJSONObject(i)))
                .collect(Collectors.toList());
    }
    
    @Test
    public void shouldMatch_whenStructuredRefIsFound() throws IOException,
            MatchException {
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
    public void shouldNotMatch_whenNoStructuredRefIsNotFound() throws IOException,
            MatchException {
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
    public void shouldMatch_whenUnstructuredRefIsFound() throws IOException,
            MatchException {
        
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
    public void shouldNotMatch_whenUnstructuredRefIsNotFound() throws IOException,
            MatchException {
        
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
    public void shouldPreserveReferenceOrder_whenUnstructured()
            throws IOException, MatchException {
        when(apiTestClient.getWorks(any(), any()))
                .thenReturn(extractMockItems("single-doi-response-1.json"));

        List<ReferenceData> references = sampleRefsUnstructured.stream().map(
                r -> new ReferenceData(new Reference(r))
            ).collect(Collectors.toList());

        MatchRequest request = new MatchRequest(references);
        MatchResponse response = matcher.match(request);

        Assert.assertEquals(references.size(), response.getMatchedLinks().size());
        IntStream.range(0, references.size()).forEach(i
                -> Assert.assertEquals(
                        references.get(i).getReference().getFormattedString(),
                        response.getMatchedLinks().get(i).getReferenceData()
                                .getReference().getFormattedString()));
    }
    
    @Test
    public void shouldPreserveReferenceOrder_whenStructured()
            throws IOException, MatchException {
        when(apiTestClient.getWorks(any(), any()))
                .thenReturn(extractMockItems("single-doi-response-1.json"));

        List<ReferenceData> references = sampleRefsStructured.stream().map(
                r -> new ReferenceData(r)
            ).collect(Collectors.toList());

        MatchRequest request = new MatchRequest(references);
        MatchResponse response = matcher.match(request);

        Assert.assertEquals(references.size(), response.getMatchedLinks().size());
        IntStream.range(0, references.size()).forEach(i -> 
        {
            Assert.assertEquals(
                    references.get(i).getReference().getFormattedString(),
                    response.getMatchedLinks().get(i).getReferenceData()
                            .getReference().getFormattedString());
            Assert.assertEquals(
                    references.get(i).getReference().getMetadataAsMap(),
                    response.getMatchedLinks().get(i).getReferenceData()
                            .getReference().getMetadataAsMap());
        });
    }
    
    private MatchResponse invokeMockStringRequest(String reference,
            String mockJsonFileName) throws IOException, MatchException {
        when(apiTestClient.getWorks(any(), any()))
                .thenReturn(extractMockItems(mockJsonFileName));
            
        MatchRequest request = new MatchRequest(
                Utils.parseInputReferences(InputType.STRING,
                        reference, "\r?\n"));
            
        return matcher.match(request);
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