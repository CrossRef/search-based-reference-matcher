package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Dominika Tkaczyk
 */
public class ReferenceMatcher {

    public static void main(String[] args) throws ParseException, IOException {
        Arguments arguments = new Arguments(args);
        if (!arguments.valid()) {
            arguments.printUsage();
            System.exit(1);
        }

        Double candidateMinScore = null;
        Double unstructuredMinScore = null;
        Double structuredMinScore = null;
        if (arguments.get("ct") != null) {
            candidateMinScore = Double.valueOf(arguments.get("ct"));
        }
        if (arguments.get("ut") != null) {
            unstructuredMinScore = Double.valueOf(arguments.get("ut"));
        }
        if (arguments.get("st") != null) {
            structuredMinScore = Double.valueOf(arguments.get("st"));
        }
        SBMVMatcher matcher = new SBMVMatcher(candidateMinScore, unstructuredMinScore,
                structuredMinScore);

        switch (arguments.get("i").toLowerCase()) {
            case "json": {
                String inputData = FileUtils.readFileToString(
                        new File(arguments.get("f")), "UTF-8");
                JSONArray dataset = new JSONArray(inputData);

                List<Candidate> matched =
                        StreamSupport.stream(dataset.spliterator(), true)
                        .map(s -> (s instanceof String)
                        ? matcher.match((String) s)
                        : matcher.match((JSONObject) s))
                        .collect(Collectors.toList());

                List<JSONObject> res = IntStream.range(0, dataset.length())
                        .mapToObj(i -> new JSONObject()
                        .put("reference", dataset.get(i))
                        .put("DOI", (matched.get(i) == null)
                                ? JSONObject.NULL
                                : matched.get(i).getDOI())
                        .put("score", (matched.get(i) == null)
                                ? JSONObject.NULL
                                : matched.get(i).getValidationScore()))
                        .collect(Collectors.toList());

                try ( PrintWriter out = new PrintWriter(arguments.get("o"))) {
                    new JSONArray(res).write(out, 1, 4);
                }

                break;
            }
            case "txt":
                List<String> references = FileUtils.readLines(
                        new File(arguments.get("f")), "UTF-8");
                
                List<Candidate> matched = references.parallelStream()
                        .map(s -> matcher.match((String) s))
                        .collect(Collectors.toList());

                List<JSONObject> res = IntStream.range(0, matched.size())
                        .mapToObj(i -> new JSONObject()
                        .put("reference", references.get(i))
                        .put("DOI", (matched.get(i) == null)
                                ? JSONObject.NULL
                                : matched.get(i).getDOI())
                        .put("score", (matched.get(i) == null)
                                ? JSONObject.NULL
                                : matched.get(i).getValidationScore()))
                        .collect(Collectors.toList());

                try ( PrintWriter out = new PrintWriter(arguments.get("o"))) {
                    new JSONArray(res).write(out, 1, 4);
                }
                break;
            case "refstr":
                Candidate m = matcher.match(arguments.get("s"));
                System.out.println((m == null) ? "null" : m.getDOI());
                break;
        }
    }
}
