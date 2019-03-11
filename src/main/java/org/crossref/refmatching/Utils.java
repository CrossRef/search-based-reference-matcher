package org.crossref.refmatching;

import cz.jirutka.unidecode.Unidecode;
import me.xdrop.fuzzywuzzy.FuzzySearch;

/**
 * Utility class.
 * 
 * @author Dominika Tkaczyk
 */
public class Utils {

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
}