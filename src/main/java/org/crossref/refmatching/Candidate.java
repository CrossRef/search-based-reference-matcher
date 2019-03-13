package org.crossref.refmatching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents a candidate target document.
 * 
 * @author Dominika Tkaczyk
 */
public class Candidate {

    private final JSONObject item;
    private final double searchScore;
    private double validationScore;

    public Candidate(JSONObject item) {
        this.item = item;
        this.searchScore = item.optDouble("score");
    }

    public JSONObject getItem() {
        return item;
    }

    public double getSearchScore() {
        return searchScore;
    }

    public double getValidationScore() {
        return validationScore;
    }

    public void setValidationScore(double validationScore) {
        this.validationScore = validationScore;
    }

    public double getValidationSimilarity(Reference reference) {
        return (reference.getType().equals(ReferenceType.STRUCTURED))
            ? getStructuredValidationSimilarity(reference)
            : getStringValidationSimilarity(reference);
    }

    public double getStringValidationSimilarity(Reference reference) {
        String refString = reference.getFormattedString();

        GenJaccardSimilarity similarity = new GenJaccardSimilarity();

        // weights for relevance score
        similarity.update("score", getScore() / 100,
                Math.max(1., getScore() / 100));

        // weights for normalized relevance score
        similarity.update("score_norm", getScore() / refString.length(),
                Math.max(1., getScore() / refString.length()));

        // remove DOI and arXiv from reference string
        // this is done to leave only bibliographic numbers in the string,
        // since all additional numbers present in the string lower the similarity
        refString = refString.replaceFirst(
                "(?<!\\d)10\\.\\d{4,9}/[-\\._;\\(\\)/:a-zA-Z0-9]+", "");
        refString = refString.replaceFirst("(?<![a-zA-Z0-9])arXiv:[\\d\\.]+", "");
        refString = refString.replaceFirst("\\[[^\\[\\]]*\\]", "").trim();

        // complete last page if abbreviated
        // changes "1425-37" to "1425-1437"
        Matcher pages = Pattern.compile(
                "\\d+[\u002D\u00AD\u2010\u2011\u2012\u2013\u2014\u2015\u207B"
                + "\u208B\u2212-]\\d+")
                .matcher(refString);
        StringBuffer sb = new StringBuffer();
        while (pages.find()) {
            pages.appendReplacement(sb, Utils.completeLastPage(pages.group()));
        }
        pages.appendTail(sb);
        refString = sb.toString();

        // extract all number appearing in the ref string
        Matcher number = Pattern.compile("(?<!\\d)\\d+(?!\\d)")
                .matcher(refString.substring(Math.min(5, refString.length())));
        List<String> numbers = new ArrayList<>();
        while (number.find()) {
            numbers.add(number.group());
        }
        
        if (numbers.isEmpty()) {
            return 0.;
        }

        // if volume equals year, but only one instance is present
        // in the reference string, add another copy
        if (getVolume() != null && getVolume().equals(getYear())
                && Collections.frequency(numbers, getVolume()) == 1) {
            numbers.add(getVolume());
        }

        // weights for volume
        if (getVolume() != null) {
            updateWeightsAll("volume", getVolume(), numbers, similarity);
        }

        // weights for year
        if (getYear() != null) {
            updateWeightsAll("year", getYear(), numbers, similarity);
        }

        // weights for issue
        if (getIssue() != null) {
            updateWeightsAll("issue", getIssue(), numbers, similarity);
        }

        // weights for pages
        if (getPage() != null) {
            updateWeightsAll("page", getPage(), numbers, similarity);
        }

        // weights for title
        if (getTitle() != null) {
            updateWeightsAll("title", getTitle(), numbers, similarity);
        }

        // weights for container-title
        if (getContainerTitle() != null) {
            updateWeightsAll("ctitle", getContainerTitle(), numbers, similarity);
        }

        // weights for author
        if (getAuthor() != null) {
            String a = Utils.normalize(getAuthor());
            String b = Utils.normalize(refString);
            b = b.substring(0, Math.min(3 * a.length(), b.length()));
            similarity.update("author", 1.,
                              Utils.stringSimilarity(a, b, false, true));
        } else if (getEditor() != null) {
            String a = Utils.normalize(getEditor());
            String b = Utils.normalize(refString);
            b = b.substring(0, Math.min(3 * a.length(), b.length()));
            similarity.update("author", 1.,
                              Utils.stringSimilarity(a, b, false, true));
        }

        // if year wasn't found, try with year +- 1
        if (getYear() != null && similarity.getMinWeight("year_0") != null
                && similarity.getMinWeight("year_0") < 1) {
            String yearPrev = String.valueOf(Integer.valueOf(getYear()) - 1);
            String yearNext = String.valueOf(Integer.valueOf(getYear()) + 1);
            if (numbers.contains(yearPrev)) {
                similarity.update("year_0", 1., 0.5);
                numbers.remove(yearPrev);
            } else if (numbers.contains(yearNext)) {
                similarity.update("year_0", 1., 0.5);
                numbers.remove(yearNext);
            }
        }

        int support = 0;
        if (getTitle() != null) {
            String a = getTitle();
            String b = refString;
            if (Utils.stringSimilarity(a, b, true, true) > 0.7) {
                support++;
            }
        }
        if (similarity.getMinWeight("year_0") != null
                && similarity.getMinWeight("year_0") > 0) {
            support++;
        }
        if (similarity.getMinWeight("volume_0") != null
                && similarity.getMinWeight("volume_0") > 0) {
            support++;
        }
        if (similarity.getMinWeight("author") != null
                && similarity.getMinWeight("author") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("page_0") != null
                && similarity.getMinWeight("page_0") > 0) {
            support++;
        }
        if (support < 3) {
            return 0.;
        }

        // weights for the remaining numbers in the ref string
        for (int i = 0; i < numbers.size(); i++) {
            similarity.update("rest_" + String.valueOf(i), 0., 1.);
        }

        return similarity.similarity();
    }

    public double getStructuredValidationSimilarity(Reference reference) {
        GenJaccardSimilarity similarity = new GenJaccardSimilarity();

        // weights for volume
        if (reference.getFieldValue("volume") != null
                && !"".equals(reference.getFieldValue("volume"))) {
            updateWeightsOne("volume", getVolume(),
                    reference.getFieldValue("volume"), similarity);
        }

        // weights for year
        if (reference.getFieldValue("year") != null
                && !"".equals(reference.getFieldValue("year"))) {
            updateWeightsOne("year", getYear(),
                    reference.getFieldValue("year"), similarity);
            if (similarity.getMinWeight("year") != null
                    && similarity.getMinWeight("year") < 1) {
                try {
                    int year1 = Integer.parseInt(getYear());
                    int year2 = Integer.parseInt(reference.getFieldValue("year"));
                    if (year1 + 1 == year2 || year2 + 1 == year1) {
                        similarity.update("year", 1., 0.5);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        // weights for pages
        if (reference.getFieldValue("first-page") != null
                && !"".equals(reference.getFieldValue("first-page"))) {
            updateWeightsOne("page", getPage(),
                    reference.getFieldValue("first-page"), similarity);
        }

        // weights for title
        if (reference.getFieldValue("article-title") != null
                && !"".equals(reference.getFieldValue("article-title"))) {
            String a = (getTitle() == null) ? "" : getTitle();
            String b = reference.getFieldValue("article-title");
            similarity.update("title", 1.,
                              Utils.stringSimilarity(a, b, true, false));
        }

        // weights for container title
        if (reference.getFieldValue("journal-title") != null
                && !"".equals(reference.getFieldValue("journal-title"))) {
            String a = (getContainerTitle() == null) ? "" : getContainerTitle();
            String b = reference.getFieldValue("journal-title");
            similarity.update("ctitle", 1.,
                              Utils.stringSimilarity(a, b, true, false));
        }
        
        // weights for volume title
        if (reference.getFieldValue("volume-title") != null
                && !"".equals(reference.getFieldValue("volume-title"))) {
            String a = (getTitle() == null) ? "" : getTitle();
            String b = reference.getFieldValue("volume-title");
	    double titleSim = Utils.stringSimilarity(a, b, true, false);
            a = (getContainerTitle() == null) ? "" : getContainerTitle();
            double ctitleSim = Utils.stringSimilarity(a, b, true, false);
            similarity.update("vtitle", 1., Math.max(titleSim, ctitleSim));
        }

        // weights for author
        if (reference.getFieldValue("author") != null
                && !"".equals(reference.getFieldValue("author"))) {
            String a = (getAuthor() == null) ? "" : getAuthor();
            String b = reference.getFieldValue("author");
            double authorSim = Utils.stringSimilarity(a, b, true,
                                                      b.contains(" "));
            a = (getEditor() == null) ? "" : getEditor();
            double editorSim = Utils.stringSimilarity(a, b, true,
                                                      b.contains(" "));
            similarity.update("author", 1., Math.max(authorSim, editorSim));
        }

        int support = 0;
        if (similarity.getMinWeight("title") != null
                && similarity.getMinWeight("title") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("ctitle") != null
                && similarity.getMinWeight("ctitle") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("vtitle") != null
                && similarity.getMinWeight("vtitle") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("author") != null
                && similarity.getMinWeight("author") > 0.7) {
            support++;
        }
        if (support < 1) {
            return 0.;
        }

        support = 0;
        if (similarity.getMinWeight("year") != null
                && similarity.getMinWeight("year") > 0) {
            support++;
        }
        if (similarity.getMinWeight("volume") != null
                && similarity.getMinWeight("volume") > 0) {
            support++;
        }
        if (similarity.getMinWeight("title") != null
                && similarity.getMinWeight("title") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("ctitle") != null
                && similarity.getMinWeight("ctitle") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("vtitle") != null
                && similarity.getMinWeight("vtitle") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("author") != null
                && similarity.getMinWeight("author") > 0.7) {
            support++;
        }
        if (similarity.getMinWeight("page") != null
                && similarity.getMinWeight("page") > 0) {
            support++;
        }
        if (support < 3) {
            return 0.;
        }
        if (item.getString("type").equals("book-chapter")
                && reference.getFieldValue("first-page") == null) {
            return 0.;
        }
        if (item.getString("type").equals("journal-issue")) {
            return 0.;
        }

        return similarity.similarity();
    }

    private void updateWeightsOne(String key, String string1, String string2,
            GenJaccardSimilarity similarity) {
        if (string1 != null && string2 != null) {
            Matcher number1 = Pattern.compile("(?<!\\d)\\d+(?!\\d)")
                    .matcher(string1);
            Matcher number2 = Pattern.compile("(?<!\\d)\\d+(?!\\d)")
                    .matcher(string2);
            if (number1.find() && number2.find()) {
                similarity.update(key, 1., 0.);
                if (number1.group().equals(number2.group())) {
                    similarity.update(key, 1., 1.);
                }
            } else {
                similarity.update(key, .5, 0.);
            }
        } else {
            similarity.update(key, .5, 0.);
        }
    }

    private void updateWeightsAll(String key, String string,
            List<String> refNumbers, GenJaccardSimilarity similarity) {
        if (string == null) {
            return;
        }
        Matcher number = Pattern.compile("(?<!\\d)\\d+(?!\\d)").matcher(string);
        int i = 0;
        while (number.find()) {
            similarity.update(key + "_" + i, 1., 0.);
            if (refNumbers.contains(number.group())) {
                similarity.update(key + "_" + i, 1., 1.);
                refNumbers.remove(number.group());
            }
            i++;
        }
    }

    private Double getScore() {
        return item.getDouble("score");
    }

    private String getVolume() {
        return item.optString("volume", null);
    }

    private String getIssue() {
        return item.optString("issue", null);
    }

    private String getPage() {
        return item.optString("page", null);
    }

    private String getYear() {
        JSONArray issued = item.getJSONObject("issued")
                .getJSONArray("date-parts");
        if (issued == null || issued.isEmpty()) {
            return null;
        }
        return issued.getJSONArray(0).optString(0, null);
    }

    private String getTitle() {
        if (!item.has("title") || item.getJSONArray("title").isEmpty()) {
            return null;
        }
        return item.getJSONArray("title").optString(0, null);
    }

    private String getContainerTitle() {
        if (!item.has("container-title")
                || item.getJSONArray("container-title").isEmpty()) {
            return null;
        }
        return item.getJSONArray("container-title").optString(0, null);
    }

    private String getAuthor() {
        if (!item.has("author") || item.getJSONArray("author").isEmpty()) {
            return null;
        }
        return item.getJSONArray("author").getJSONObject(0)
                .optString("family", null);
    }

    private String getEditor() {
        if (!item.has("editor") || item.getJSONArray("editor").isEmpty()) {
            return null;
        }
        return item.getJSONArray("editor").getJSONObject(0)
                .optString("family", null);
    }

    public String getDOI() {
        return item.getString("DOI");
    }

}