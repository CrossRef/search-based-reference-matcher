package org.crossref.refmatching;

import java.io.IOException;
import org.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class SBMVMatcherTest {
    
    @Test
    public void testJournals() {
        SBMVMatcher matcher = new SBMVMatcher();
        
        assertEquals(6187, matcher.journals.size());
        assertEquals("monthly notices of the royal astronomical society",
                     matcher.journals.get("mnras"));
    }

    @Test
    public void testMatchString() throws IOException {
        SBMVMatcher matcher = new SBMVMatcher();

        assertEquals("10.1007/s10032-015-0249-8", matcher.match(
                "[1]D. Tkaczyk, P. Szostek, M. Fedoryszak, P. J. Dendek, and "
                + "Ł. Bolikowski,“CERMINE: automatic extraction of structured "
                + "metadata from scientific literature,” International Journal "
                + "on Document Analysis and Recognition (IJDAR), vol. 18, no. 4, "
                + "pp. 317–335, 2015.").getDOI());
        assertEquals("10.1007/s10032-015-0249-8", matcher.match(
                "[1] D. Tkaczyk, P. Szostek, M. Fedoryszak, P.J. Dendek, Ł. "
                + "Bolikowski, International Journal on Document Analysis and "
                + "Recognition (IJDAR) 18 (2015) 317.").getDOI());

        assertNull(matcher.match(
                "P. Szostek, M. Fedoryszak, P. J. Dendek, and Ł. Bolikowski,"
                + "“CERMINE: automatic extraction of structured metadata from "
                + "scientific literature,” IJDAR, vol. 14, no. 40, pp. 310–335, "
                + "2016."));
    }

    @Test
    public void testMatchStructured() throws IOException {
        SBMVMatcher matcher = new SBMVMatcher();

        JSONObject reference = new JSONObject();
        reference.put("author", "Tkaczyk");
        reference.put("volume", "18");
        reference.put("first-page", "317");
        reference.put("year", "2015");
        reference.put("journal-title", "IJDAR");

        assertEquals("10.1007/s10032-015-0249-8",
                matcher.match(reference).getDOI());

        reference = new JSONObject();
        reference.put("author", "Tkaczyk");
        reference.put("volume", "93");
        reference.put("year", "2015");
        reference.put("journal-title",
                "Communications in Computer and Information");

        assertNull(matcher.match(reference));

    }
}
