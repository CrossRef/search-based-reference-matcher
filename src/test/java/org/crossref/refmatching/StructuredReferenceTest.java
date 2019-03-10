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
        
        assertNull(new Reference(reference).getFormattedString());
    }

    @Test
    public void testField() {
        Map<String, String> reference = new HashMap<>();
        reference.put("author", "West");
        reference.put("volume", "84");
        reference.put("first-page", "1763");
        reference.put("year", "1962");
        reference.put("journal-title", "J. Amer. chem. Soc.");
        
        Reference sr = new Reference(reference);
        
        assertNull(sr.getFieldValue("article-title"));
        assertNull(sr.getFieldValue("issue"));
        reference.keySet().forEach((key) -> {
            assertEquals(reference.get(key), sr.getFieldValue(key));
        });
    }
}