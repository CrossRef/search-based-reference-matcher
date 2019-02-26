package org.crossref.refmatching;

/**
 * Represents something that can be thought of as a reference. Generally,
 * a reference can be expressed as a string.
 * 
 * @author joe.aparo
 */
public interface Reference {
    /**
     * Return the string form of the reference.
     * 
     * @return A reference string
     */
    String getString();
}
