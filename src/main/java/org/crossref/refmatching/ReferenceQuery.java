package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a distinct reference query object, which includes the
 * original reference to query, and other values pertinent to the
 * reference w/respect to the query. Optional values are provided
 * so that caller-specified values (such as an internal correlation key)
 * can be echoed back as part of the items returned in a match response.
 * 
 * @author joe.aparo
 */
public class ReferenceQuery {
    private final Reference reference;
    private final Map<String, Object> options = new HashMap<>();
    
    public ReferenceQuery(Reference reference) {
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
