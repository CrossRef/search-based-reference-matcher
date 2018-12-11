package org.crossref.refmatching;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class GenJaccardSimilarityTest {
    
    @Test
    public void testEmpty() {
        GenJaccardSimilarity similarity = new GenJaccardSimilarity();
        
        assertNull(similarity.getMinWeight("author"));
        assertNull(similarity.getMinWeight("year"));
        
        assertEquals(1., similarity.similarity(), 0.0001);
    }
    
    @Test
    public void testSimilarity() {
        GenJaccardSimilarity similarity = new GenJaccardSimilarity();
        similarity.update("year", 1., 1.);
        similarity.update("title", 1., 0.8);
        similarity.update("volume", 1., 0.);
        
        assertNull(similarity.getMinWeight("author"));
        assertEquals(1., similarity.getMinWeight("year"), 0.0001);
        assertEquals(0.8, similarity.getMinWeight("title"), 0.0001);
        
        assertEquals(0.6, similarity.similarity(), 0.0001);
    }
}
