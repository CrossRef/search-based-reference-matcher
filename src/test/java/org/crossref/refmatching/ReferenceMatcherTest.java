package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.utils.ResourceUtils;
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
    private MatchRequest request = new MatchRequest();
    private Map<String, String> apiResponseMap = new HashMap<String, String>();
    
    @Mock
    ICrossRefApiClient apiClient;
    
    ReferenceMatcher matcher;
    
    @Before
    public void setupMock() {
        loadSampleResponseMap();
        
        MockitoAnnotations.initMocks(this);
        
        matcher = new ReferenceMatcher(apiClient);
        matcher.setCacheJournals(true);
        matcher.initialize();
    }
    
    @Test
    public void shouldReturnMatch_whenCandidatesFound() {
        try {
            when(apiClient.getWorks(any())).thenReturn(apiResponseMap.get("api-response-random.json"));
            request.setInputType(RequestInputType.STRING);
            request.setRefString("van Staal, C. R., Ravenhurst, C. E., Winchester, J. A., Roddick, J. C., and Langton, J. P., 1990, Post-Taconic blueschist suture in the northern Appalachians of northern New Brunswick, Canada: Geology, v. 18, p. 1073-1077.");
            matcher.match(request);
            
        } catch (IOException ex) {
            Logger.getLogger(ReferenceMatcherTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void loadSampleResponseMap() {
        File[] files = ResourceUtils.getResourceFolderFiles("api-responses");
        for (File f : files) {
            try {
                apiResponseMap.put(f.getName(), FileUtils.readFileToString(f, "UTF-8"));
            } catch (IOException ex) {
                Logger.getLogger(ReferenceMatcherTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}