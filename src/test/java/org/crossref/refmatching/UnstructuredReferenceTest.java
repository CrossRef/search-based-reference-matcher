package org.crossref.refmatching;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class UnstructuredReferenceTest {

    @Test
    public void testString() {
        assertEquals("", new UnstructuredReference("").getString());
        assertEquals("Reference String.",
                new UnstructuredReference("Reference String.").getString());
    }

    @Test
    public void testField() {
        assertNull(new UnstructuredReference("").getField("key"));
        assertNull(new UnstructuredReference("Reference String.")
                .getField("anotherkey"));
    }

}
