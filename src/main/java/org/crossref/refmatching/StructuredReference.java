package org.crossref.refmatching;

import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 *
 * @author Dominika Tkaczyk
 */
public class StructuredReference {
    
    private final Map<String, String> metadata;

    public StructuredReference(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public StructuredReference(JSONObject metadata) {
        this.metadata = metadata.toMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> (entry.getValue() instanceof Integer)
                            ? String.valueOf(entry.getValue())
                            : (String) entry.getValue()));
    }

    @Override
    public String toString() {
        String string = "";
        for (String key : new String[]{"author", "article-title", "journal-title",
            "series-title", "volume-title", "year", "volume", "issue",
            "first-page", "edition", "ISSN"}) {
            string = string + metadata.getOrDefault(key, "") + " ";
        }
        return string.replaceAll(" +", " ").trim();
    }

    public String getField(String key) {
        return metadata.get(key);
    }
}
