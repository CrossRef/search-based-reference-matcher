package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Represents a structured reference. Internally it is managed as a
 * map of allowable key value pairs for fielded access, and inherits
 * basic string support from UnstructuredReference.
 *
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class StructuredReference extends UnstructuredReference {
    
    private final Map<String, String> metadata;

    public StructuredReference(Map<String, String> metadata) {
        super(createRefStringFromMap(metadata));
        this.metadata = metadata;
    }
    
    public StructuredReference(JSONObject metadata) {
        this(metadata.toMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> (entry.getValue() instanceof Integer)
                            ? String.valueOf(entry.getValue())
                            : (String) entry.getValue())));
    }

    public String getField(String key) {
        return metadata.get(key);
    }
    
    /**
     * Return a copy of the internal map.
     * 
     * @return A map
     */
    public Map<String, String> getMap() {
        Map<String, String> map = new HashMap<>();
        map.putAll(metadata);
        
        return map;
    }
    
    private static String createRefStringFromMap(Map<String, String> metadata) {
        StringBuilder buf = new StringBuilder(500);
        for (String key : new String[]{"author", "article-title", "journal-title",
            "series-title", "volume-title", "year", "volume", "issue",
            "first-page", "edition", "ISSN"}) {
            
            if (buf.length() > 0) {
                buf.append(" ");
            }
            buf.append(metadata.getOrDefault(key, ""));
        }
        return buf.toString().replaceAll(" +", " ").trim();
    }
    
    public ReferenceType getType() {
        return ReferenceType.STRUCTURED;
    }
}
