package org.crossref.refmatching;

import java.util.stream.IntStream;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class MatchResponseTest {

    @Test
    public void testSynchronizedAddMatchedLink() {
        MatchResponse response = new MatchResponse(null);
        IntStream.range(0, 1000).parallel()
                .forEach(n -> {
                    for (int i = 0; i < 100; i++) {
                        response.addMatchedLink(null);
                    }
                });
        assertEquals(100000, response.getMatchedLinks().size());
    }

}