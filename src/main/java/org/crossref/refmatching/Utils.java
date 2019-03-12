package org.crossref.refmatching;

import cz.jirutka.unidecode.Unidecode;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.crossref.common.utils.JsonUtils;
import org.crossref.common.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class.
 * 
 * @author Dominika Tkaczyk
 * @author Joe Aparo
 */
public class Utils {
    
    private static final Logger LOGGER = LogUtils.getLogger();

    public static String normalize(String string) {
        string = Unidecode.toAscii().decode(string).toLowerCase();
        return string.replaceAll("\\[\\?\\]", "?");
    }

    public static double stringSimilarity(String string1, String string2,
            boolean normalize, boolean partial) {
        if (normalize) {
            string1 = normalize(string1);
            string2 = normalize(string2);
        }
        if (partial) {
            return (double) FuzzySearch.partialRatio(string1, string2) / 100;
        }
        return (double) FuzzySearch.ratio(string1, string2) / 100;
    }

    public static String completeLastPage(String pages) {
        String[] numbers = pages.split("[^\\d]");
        String first = numbers[0];
        String last = numbers[1];
        if (first.length() > last.length()
                && Integer.valueOf(
                        first.substring(first.length() - last.length()))
                <= Integer.valueOf(last)) {
            return first + "-"
                    + first.substring(0, first.length() - last.length()) + last;
        }
        return pages;
    }
    
    /**
     * Loads standard headers (mail and Metadata Plus key) from a local file.
     * The headers will be sent by the API client to the server when making
     * HTTP calls.
     *
     * @param apiKeyFile The path to the key file 
     * @return A map of headers
     */
    public static Map<String, String> createStdHeaders(String apiKeyFile) {
        String crapiData;
        try {
            crapiData = FileUtils.readFileToString(new File(apiKeyFile), "UTF-8");
        } catch (IOException ex) {
            LOGGER.warn("Unable to read API key file: " + apiKeyFile);
            return null;
        }
        
        JSONObject crapiJson = new JSONObject(crapiData);
        String authorization = crapiJson.optString("Authorization", null);
        String mailTo = crapiJson.optString("Mailto", null);
        
        Map<String, String> stdHeaders = new HashMap();
        
         if (!StringUtils.isEmpty(authorization)) {
            stdHeaders.put("Authorization", authorization);
        }

        if (!StringUtils.isEmpty(mailTo)) {
            stdHeaders.put("Mailto", mailTo);
        }
        
        return stdHeaders;
    }
    
    public static List<ReferenceData> parseInputReferences(InputType inputType,
            String input, String delimiter) throws IOException {
        String data;
        if (inputType == InputType.FILE) {
            data = FileUtils.readFileToString(new File(input), "UTF-8");
        } else {
            data = input;
        }
        try {
            JSONArray arr = JsonUtils.createJSONArray(data);
            List<Object> refs = StreamSupport.stream(arr.spliterator(), true)
                                    .collect(Collectors.toList());
            return refs.parallelStream()
                        .map(s -> (s instanceof String)
                                ? new Reference((String) s)
                                : new Reference((JSONObject) s))
                        .map(r -> new ReferenceData(r))
                        .collect(Collectors.toList());
        } catch (JSONException ex) {
            List<String> strs = Arrays.asList(data.split(delimiter));
            return strs.parallelStream()
                    .map(s -> {
                        try {
                            JSONObject refObject = new JSONObject(s);
                            return new Reference(refObject);
                        } catch (JSONException ex1) {
                            return new Reference(s);
                        }
                    })
                    .map(r -> new ReferenceData(r))
                    .collect(Collectors.toList());
        }
    }
    
}