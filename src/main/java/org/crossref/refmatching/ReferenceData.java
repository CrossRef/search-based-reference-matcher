package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the bibliographic reference and additional related information
 * (optional). Additional information are not used for matching, but preserved
 * and returned back in the response. Example use case is an internal correlation
 * key.
 * 
 * @author Joe Aparo
 */
public class ReferenceData {
    private final Reference reference;
    private final Map<String, Object> options = new HashMap<>();
    
    public ReferenceData(Reference reference) {
        this.reference = reference;
    }
    
    public Reference getReference() {
        return reference;
    }
    
    public Object getOption(String key) {
        return options.get(key);
    }
    
    public void putOption(String key, Object value) {
        options.put(key, value);
    }
}