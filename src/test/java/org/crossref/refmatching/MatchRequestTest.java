package org.crossref.refmatching;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Assert;

/**
 *
 * @author Dominika Tkaczyk
 */
public class MatchRequestTest {
    
    @Mock
    ReferenceMatcher matcherMock;

    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void numThreadsTest() {
        MatchRequest request = new MatchRequest(
            IntStream.range(0, 10)
                    .mapToObj(i -> new ReferenceData(new Reference("")))
                    .collect(Collectors.toList())
        );
        Assert.assertEquals(MatchRequest.DEFAULT_NUM_THREADS,
                request.getNumThreads());

        request.setNumThreads(-10);
        Assert.assertEquals(1, request.getNumThreads());
        
        request.setNumThreads(0);
        Assert.assertEquals(1, request.getNumThreads());
        
        request.setNumThreads(7);
        Assert.assertEquals(7, request.getNumThreads());
        
        request.setNumThreads(15);
        Assert.assertEquals(10, request.getNumThreads());
        
        request = new MatchRequest(
            IntStream.range(0, 100)
                    .mapToObj(i -> new ReferenceData(new Reference("")))
                    .collect(Collectors.toList())
        );
        
        request.setNumThreads(MatchRequest.MAX_THREADS + 10);
        Assert.assertEquals(MatchRequest.MAX_THREADS, request.getNumThreads());
    }

}
