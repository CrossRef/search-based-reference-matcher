package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the ways in which textual content to be matched may be provided.
 * 
 * FILE - content to match is specified as the name of a file to read
 * STRING - content to match is provided directly
 */
public enum InputType {
    FILE("file"),
    STRING("string"); 
    
    private static final Map<String, InputType> typesByCode = new HashMap<>();
    private final String code;
    
    static {
        typesByCode.put(FILE.getCode(), FILE);
        typesByCode.put(STRING.getCode(), STRING);
    }
    
    /**
     * Construct the enum.
     * @param code User specified code
     */
    InputType(String code) {
        this.code = code;
    }
    
    /**
     * Fetch a type by its code.
     * 
     * @param code Code to find
     * @return Found type, or null if not found
     */
    public static InputType getByCode(String code) {
        return typesByCode.get(code);
    }
    
    /**
     * Get the internal code associated with the enum.
     * @return A user defined string code
     */
    public String getCode() {
        return this.code;
    }
}
