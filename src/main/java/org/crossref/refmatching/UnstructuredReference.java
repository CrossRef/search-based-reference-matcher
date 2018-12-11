package org.crossref.refmatching;

/**
 *
 * @author Dominika Tkaczyk
 */
public class UnstructuredReference extends Reference {
    
    private final String string;

    public UnstructuredReference(String string) {
        this.string = string;
    }

    @Override
    public String getString() {
        return string;
    }
    
    @Override
    public String getField(String key) {
        return null;
    }

}
