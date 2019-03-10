package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Represents a reference.
 * 
 * @author Dominika Tkaczyk
 */
public class Reference {
    
    private final Map<String, String> metadata;
    private final String formattedString;
    private final ReferenceType type;
    
    public Reference(String formattedString) {
        this(new HashMap<>(), formattedString, ReferenceType.UNSTRUCTURED);
    }
    
    public Reference(Map<String, String> metadata) {
        this(metadata, null, ReferenceType.STRUCTURED);
    }
    
    public Reference(JSONObject metadata) {
        this(metadata, null, ReferenceType.STRUCTURED);
    }
    
    public Reference(JSONObject metadata, String formattedString,
            ReferenceType type) {
        this(metadata.toMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> (entry.getValue() instanceof Integer)
                            ? String.valueOf(entry.getValue())
                            : (String) entry.getValue())),
            formattedString, type);
    }
    
    public Reference(Map<String, String> metadata, String formattedString,
            ReferenceType type) {
        this.metadata = metadata;
        this.formattedString = formattedString;
        this.type = type;
    }

    public String getFormattedString() {
        return formattedString;
    }
    
    public String getFieldValue(String fieldName) {
        return metadata.get(fieldName);
    }

    public ReferenceType getType() {
        return type;
    }

    public Reference withField(String fieldType, String fieldValue) {
        Map<String, String> newMetadata = new HashMap<>(metadata);
        newMetadata.put(fieldType, fieldValue);
        return new Reference(newMetadata, formattedString, type);
    }

    public JSONObject getMetadataAsJSON() {
        return new JSONObject(metadata);
    }
    
}