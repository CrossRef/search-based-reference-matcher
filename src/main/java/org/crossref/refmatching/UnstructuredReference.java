package org.crossref.refmatching;

/**
 * Represents a simple, unstructured string reference.
 * 
 * @author joe.aparo
 */
public class UnstructuredReference implements Reference {
    private final String reference;
    
    public UnstructuredReference(String reference) {
        this.reference = reference;
    }
    
    @Override
    public String getString() {
        return reference;
    }
    
    /**
     * Delegate to getString()
     * @return The reference string
     */
    @Override 
    public String toString() {
        return getString();
    }
    
    public ReferenceType getType() {
        return ReferenceType.UNSTRUCTURED;
    }
}
