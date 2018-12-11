package org.crossref.refmatching;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Dominika Tkaczyk
 */
public class Arguments {

    private final Options options;
    private final CommandLine cmd;

    public Arguments(String[] args) throws ParseException {
        options = new Options();
        options.addOption("i", "inputtype", true,
                "input type; valid values are: \"refstr\" (a single unstructured "
                + "reference), \"txt\" (a file with unstructured reference "
                + "strings, one per line), \"json\" (JSON file with a list of "
                + "reference strings and/or structured references)");
        options.addOption("f", "file", true,
                "input file path, used with \"txt\" and \"json\" input types");
        options.addOption("o", "output", true,
                "output file path, used with \"txt\" and \"json\" input types");
        options.addOption("s", "string", true,
                "reference string, used with \"refstr\" file type");
        options.addOption("ct", true, "candidate selection normalized threshold");
        options.addOption("ut", true, "unstructured validation threshold");
        options.addOption("st", true, "structured validation threshold");

        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, args);
    }

    public String get(String name) {
        return cmd.getOptionValue(name);
    }

    public boolean valid() {
        return errorMessage() == null;
    }

    public String errorMessage() {
        if (!cmd.hasOption("i")) {
            return "Input type not given";
        }
        if (!cmd.getOptionValue("i").equals("refstr")
                && !cmd.getOptionValue("i").equals("txt")
                && !cmd.getOptionValue("i").equals("json")) {
            return "Illegal input type; valid values are: \"refstr\", \"txt\", "
                    + "\"json\"";
        }
        if (cmd.getOptionValue("i").equals("txt") 
                || cmd.getOptionValue("i").equals("json")) {
            if (!cmd.hasOption("f")) {
                return "Input file path has to be provided for this input type.";
            }
            if (!cmd.hasOption("o")) {
                return "Output file path has to be provided for this input type.";
            }
        }
        if (cmd.getOptionValue("i").equals("refstr")) {
            if (!cmd.hasOption("s")) {
                return "Reference has to be provided for this input type.";
            }
        }
        if (cmd.hasOption("ct")) {
            try {
                Double.valueOf(cmd.getOptionValue("ct"));
            } catch (NumberFormatException e) {
                return "Threshold value has to be double";
            }
        }
        if (cmd.hasOption("ut")) {
            try {
                Double.valueOf(cmd.getOptionValue("ut"));
            } catch (NumberFormatException e) {
                return "Threshold value has to be double";
            }
        }
        if (cmd.hasOption("st")) {
            try {
                Double.valueOf(cmd.getOptionValue("st"));
            } catch (NumberFormatException e) {
                return "Threshold value has to be double";
            }
        }
        return null;
    }

    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        String syntax = "Matcher -i inputtype [-f input file path] "
                + "[-o output file path] [-s ref string]";
        if (errorMessage() != null) {
            System.out.println(errorMessage());
        }
        formatter.printHelp(syntax, options);
    }

}
