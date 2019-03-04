package org.crossref.refmatching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.rest.impl.CrossRefApiHttpClient;
import org.crossref.common.utils.LogUtils;
import org.crossref.common.utils.UnmanagedHttpClient;
import org.json.JSONObject;

/**
 * This class executes a matching request via a main application entrypoint.
 * 
 * @author joe.aparo
 */
public class MainApp {
    private static final String DEFAULT_API_SCHEME = "https";
    private static final String DEFAULT_API_HOST = "api.crossref.org";
    private static final int DEFAULT_API_PORT = 8011;
    private final static String CRAPI_KEY_FILE = ".crapi_key";
    private static final Logger logger = LogUtils.getLogger();
    
    private static String apiScheme = DEFAULT_API_SCHEME;
    private static String apiHost = DEFAULT_API_HOST;
    private static int apiPort = DEFAULT_API_PORT;
    private static String apiKeyFile = System.getProperty("user.home") + "/" + CRAPI_KEY_FILE;
    private static String outputFileName = null;

    public static void main(String[] args) {
        try {
            MatchRequest request = processArgs(args);

            // Initialize API client connector
            UnmanagedHttpClient httpClient = new UnmanagedHttpClient(apiScheme, apiHost, apiPort);
            httpClient.initialize();
            httpClient.setCommonHeaders(createStdHeaders());
            ICrossRefApiClient apiClient = new CrossRefApiHttpClient(httpClient);
            
            // Initialize matcher object
            ReferenceMatcher matcher = new ReferenceMatcher(apiClient);
            matcher.setCacheJournals(true);
            matcher.initialize();
            
            // Get match results
            outputResults(matcher.match(request));
        } catch (IOException ex) {
            logger.error("Error performing matching process: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * This method will attempt to load standard header info from a local file.
     * The headers will be sent by the api client to the server when making 
     * http calls.
     * 
     * @return A map of headers
     */
    private static Map<String, String> createStdHeaders() {
        String crapiData;

        try {
            crapiData = FileUtils.readFileToString(new File(apiKeyFile), "UTF-8");
        } catch (IOException ex) {
            logger.warn("Unable to read API key file: " + apiKeyFile);
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
    
    /**
     * Initialize a matching request object from given input arguments as well
     * as other runtime variables. In the process, this method performs validation
     * and will exit the program if a validation fails.
     * 
     * @param args The arguments to process
     */
    private static MatchRequest processArgs(String[] args) {
        
        // Define acceptable options
        Options options = new Options();
        options.addOption("it", "input-type", true,
            "This option affects how the -i option is interpreted. Valid values for this option are "
            + "\"string\" and \"file\". If \"string\", the value of the -i option is the actual data "
            + "to perform a match on. If \"file\", the value of the -i option is interprereted as "
            + "the name of a file from which to read the data to perform a match on. In either case, "
            + "textual input is assumed to be in one of two formats, either 1) A JSON Array of structured "
            + "references, or 2) A delmited string of reference strings. See the -d option regarding "
            + "the delimiter. In the 2nd case, strings can be either unstructured, or structured JSON"
            + "references.");
        options.addOption("i", "input", true,
            "A string value to be interpreted based on the value of the -it option");
        options.addOption("ct", "cand-min", true, "Candidate selection normalized threshold");
        options.addOption("ut", "unstr-min", true, "Unstructured validation threshold");
        options.addOption("st", "str-min", true, "Structured validation threshold");
        options.addOption("ur", "unstr-rows", true, "Number of candidates to consider for an unstructured match");
        options.addOption("sr", "str-rows", true, "Number of candidates to consider for an structured match");
        options.addOption("as", "api-scheme", true, "CR API http scheme (http or https)");
        options.addOption("ah", "api-host", true, "CR API host)");
        options.addOption("ap", "api-port", true, "CR API port");
        options.addOption("ak", "key-file", true, "CR API key file");
        options.addOption("d", "delim", true, "Textual data delimiter");
        options.addOption("o", "out-file", true, "File to direct console output to");
        options.addOption("m", "mail-to", true, "Mail-To option for 'polite' API call");
        options.addOption("h", "help", false, "Print help");
      
       // Parse/validate given arguments against defined options
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        
        // Initialize a request object from the parsed options
        try {
            cmd = parser.parse(options, args);
            
            if (cmd.hasOption("h")) {
                printHelp(options);
            }
            
            // Check required input type option
            if (!cmd.hasOption("it")) {
               throw new RuntimeException("Input type not specified");
            }

             // Check required input value option
            if (!cmd.hasOption("i")) {
               throw new RuntimeException("Input value not specified");
            }

           // Validate given input type
            String typeCode = cmd.getOptionValue("it");
            InputType inputType = InputType.getByCode(typeCode);
            if (inputType == null) {
                List<String> okVals = Arrays.asList(
                    InputType.values()).stream().map(o-> {return o.getCode();}).collect(Collectors.toList());
 
                throw new RuntimeException("Invalid input type specified: " + typeCode + ". Valid types are: " + okVals);
            }
            
            String inputValue = cmd.getOptionValue("i");
            if (inputType == InputType.FILE) {
                // Check for input file
                File file = new File(inputValue);
                if (!file.exists()) {
                    throw new RuntimeException("The specified input file does not exist: " + inputValue);
                }
            }

            // Minimum candidate score
            double candidateMinScore = MatchRequest.DEFAULT_CAND_MIN_SCORE;
            if (cmd.hasOption("ct")) {
               candidateMinScore = Double.valueOf(cmd.getOptionValue("ct"));
            }
            
            // Minimum unstructured matching score
            double unstructuredMinScore = MatchRequest.DEFAULT_UNSTR_MIN_SCORE;
            if (cmd.hasOption("ut")) {
               unstructuredMinScore = Double.valueOf(cmd.getOptionValue("ut"));
            }
            
            // Minimum structured matching score
            double structuredMinScore = MatchRequest.DEFAULT_STR_MIN_SCORE;
            if (cmd.hasOption("st")) {
               structuredMinScore = Double.valueOf(cmd.getOptionValue("st"));
            }
            
            if (cmd.hasOption("as")) {
               apiScheme = cmd.getOptionValue("as").toLowerCase();
               if (!(apiScheme.equals("http") || apiScheme.equals("https"))) {
                   throw new RuntimeException("Invalid http scheme: " + apiScheme);
               }
            }
            
            // Minimum structured matching score
            int structuredRows = MatchRequest.DEFAULT_STR_ROWS;
            if (cmd.hasOption("sr")) {
               structuredRows = Integer.valueOf(cmd.getOptionValue("sr"));
            }
            
            // Minimum structured matching score
            int unstructuredRows = MatchRequest.DEFAULT_UNSTR_ROWS;
            if (cmd.hasOption("ur")) {
               unstructuredRows = Integer.valueOf(cmd.getOptionValue("ur"));
            }

            if (cmd.hasOption("ah")) {
               apiHost = cmd.getOptionValue("ah");
            }
            
            if (cmd.hasOption("ap")) {
               apiPort = Integer.valueOf(cmd.getOptionValue("ap"));
            } 
            
            if (cmd.hasOption("ak")) {
               apiKeyFile = cmd.getOptionValue("ak");
            } 

            if (cmd.hasOption("o")) {
               outputFileName = cmd.getOptionValue("o");
            } 

            String mailTo = null;
            if (cmd.hasOption("m")) {
               mailTo = cmd.getOptionValue("m");
            }

            // Return initialized request
            MatchRequest request = new MatchRequest(inputType, inputValue);
            
            request.setCandidateMinScore(candidateMinScore);
            request.setUnstructuredMinScore(unstructuredMinScore);
            request.setStructuredMinScore(structuredMinScore);
            request.setUnstructuredRows(unstructuredRows);
            request.setStructuredRows(structuredRows);
            request.setMailTo(mailTo);
            
            return request;
            
        } catch (RuntimeException | ParseException ex) {
            logger.error("Error processing input arguments: " + ex.getMessage());
            printHelp(options);
            return null;
        }
    }
    
    /**
     * Display command line option help text.
     * @param options Available options
     */
    private static void printHelp(Options options) {
        System.out.println("\nUsage: MainApp [options]");
        
        System.out.println("\nOptions:");
        String fmt = "%-15s%-15s%s";
        
        System.out.println(String.format(fmt, "Short Opt", "Long Opt", "Description"));
        System.out.println(String.format(fmt, "---------", "--------", "-----------"));
        
        options.getOptions().stream().forEach(o -> {
            System.out.println(
                String.format(fmt, "-"+ o.getOpt(), "--" + o.getLongOpt(), o.getDescription()));
        });
                
        System.exit(0);
    }
    
    /**
     * Output results.
     * @param response The response containing results
     */
    private static void outputResults(MatchResponse response) {
        JSONObject outputItem = new JSONObject();
        
        // Assume console
        OutputStream out = System.out;
        
        // Check for redirect to file
        if (outputFileName != null) {
            try {
                out = new FileOutputStream(new File(outputFileName));
            } catch (FileNotFoundException ex) {
                java.util.logging.Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // Generate output
        try (PrintWriter writer = new PrintWriter(out)) {
            int x = 0; // for commas
            
            writer.println("[");
            for (ReferenceLink m : response.getMatches()) {
                if (x++ > 0) {
                    writer.println(',');
                }
                
                outputItem.put("doi", m.getDOI());
                outputItem.put("score", m.getScore());
                outputItem.put("reference", m.getReference());
                
                writer.print(outputItem.toString());
            }
            
            // Close the writer
            writer.println("\n]");
            writer.flush();
        }
    }
}
