/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.crossref.refmatching;

import java.util.ArrayList;
import java.util.List;
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
 * @author joe.aparo
 */
public class RefMatchThreadPoolTest {
    @Mock
    ICrossRefApiClient apiTestClient;
    
    ReferenceMatcher matcher;
    List<String> sampleRefs = new ArrayList<>();
    JSONArray dummyApiResp = null;
    
    @Before
    public void setupMock() {
        // Cache mock API responses
        MockitoAnnotations.initMocks(this);
        sampleRefs = ResourceUtils.readResourceAsLines("/test-inputs/sample-ref-strings-2000.txt");
        JSONObject json = new JSONObject(ResourceUtils.readResourceAsString("/api-responses/single-doi-response-1.json"));
        dummyApiResp = json.getJSONObject("message").optJSONArray("items");
        
        matcher = new ReferenceMatcher(apiTestClient);
        matcher.setCacheJournalAbbrevMap(true);
        matcher.initialize();
    }
    
    @Test
    public void shouldProcessAllInOrder_forGivenReferences() {
        try {
            when(apiTestClient.getWorks(any(), any())).thenReturn(dummyApiResp);
            
            List<ReferenceData> references = new ArrayList<>();
            sampleRefs.stream().forEach(r -> {
               references.add(new ReferenceData(new Reference(r)));
            });
            
            MatchRequest request = new MatchRequest(references);
            MatchResponse response = matcher.match(request);
            
            Assert.assertTrue(response.getMatchedLinks().size() == references.size());
            
            Assert.assertTrue(
                references.get(0).getReference().getFormattedString().equals(
                    response.getMatchedLinks().get(0).getReferenceData().getReference().getFormattedString()));
            
            Assert.assertTrue(
                references.get(0).getReference().getFormattedString().equals(
                    response.getMatchedLinks().get(0).getReferenceData().getReference().getFormattedString()));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
