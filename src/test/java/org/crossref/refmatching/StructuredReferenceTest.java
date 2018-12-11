package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class StructuredReferenceTest {
    
    @Test
    public void testString() {
        Map<String, String> reference = new HashMap<>();
        reference.put("author", "West");
        reference.put("volume", "84");
        reference.put("first-page", "1763");
        reference.put("year", "1962");
        reference.put("journal-title", "J. Amer. chem. Soc.");
        
        assertEquals("West J. Amer. chem. Soc. 1962 84 1763",
                     new StructuredReference(reference).getString());
    }

    @Test
    public void testField() {
        Map<String, String> reference = new HashMap<>();
        reference.put("author", "West");
        reference.put("volume", "84");
        reference.put("first-page", "1763");
        reference.put("year", "1962");
        reference.put("journal-title", "J. Amer. chem. Soc.");
        
        StructuredReference sr = new StructuredReference(reference);
        
        assertNull(sr.getField("article-title"));
        assertNull(sr.getField("issue"));
        reference.keySet().forEach((key) -> {
            assertEquals(reference.get(key), sr.getField(key));
        });
    }
    
}
