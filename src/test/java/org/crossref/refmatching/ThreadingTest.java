package org.crossref.refmatching;

import java.io.IOException;
import java.util.concurrent.Semaphore;
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
 */
public class ThreadingTest {
    
    @Mock
    ReferenceMatcher matcherMock;

    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(timeout = 60000)
    public void shouldRunInParallel() throws InterruptedException {
        Semaphore reportSem = new Semaphore(0);
        Semaphore ackSem = new Semaphore(0);

        Thread t = new Thread() {
            public void run() {
                try {
                    String references =
                            "ref1\n{\"ref\":\"2\"}\nref3\nref4\n{\"ref\":\"5\"}";
                    MatchRequest request = new MatchRequest(
                            Utils.parseInputReferences(InputType.STRING,
                                    references, "\r?\n"));
                    request.setNumThreads(5);

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
                } catch (IOException | MatchException ex) {
                    throw new RuntimeException(ex);
                }
            }

        };
        t.start();

        reportSem.acquire(5);
        ackSem.release(5);
    }

}
