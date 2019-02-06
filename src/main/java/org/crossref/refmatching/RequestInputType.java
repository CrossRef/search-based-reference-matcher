package org.crossref.refmatching;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the kinds of input the user may provide for a match request.
 */
public enum RequestInputType {
    JSON_FILE("json"),
    TEXT_FILE("txt"),
    STRING("refstr");
    
    private static Map<String, RequestInputType> typesByCode = new HashMap<>();
    private String code;
    
    static {
        typesByCode.put(JSON_FILE.getCode(), JSON_FILE);
        typesByCode.put(TEXT_FILE.getCode(), TEXT_FILE);
        typesByCode.put(STRING.getCode(), STRING);
    }
    
    /**
     * Construct the enum.
     * @param code User specified code
     */
    RequestInputType(String code) {
        this.code = code;
    }
    
    /**
     * Fetch a type by its code.
     * 
     * @param code Code to find
     * @return Found type, or null if not found
     */
    public static RequestInputType getByCode(String code) {
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
