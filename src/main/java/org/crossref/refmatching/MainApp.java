package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.rest.impl.CrossRefApiHttpClient;
import org.crossref.common.utils.LogUtils;
import org.crossref.common.utils.UnmanagedHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class executes a matching request via a main application entrypoint.
 * 
 * @author Joe Aparo
 */
public class MainApp {
    private static final String DEFAULT_API_SCHEME = "https";
    private static final String DEFAULT_API_HOST = "api.crossref.org";
    private static final int DEFAULT_API_PORT = 443;
    private final static String CRAPI_KEY_FILE = ".crapi_key";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static String apiScheme = DEFAULT_API_SCHEME;
    private static String apiHost = DEFAULT_API_HOST;
    private static int apiPort = DEFAULT_API_PORT;
    private static String apiKeyFile = System.getProperty("user.home") + "/"
            + CRAPI_KEY_FILE;
    private static String outputFileName = null;

    public static void main(String[] args) {
        try {
            MatchRequest request = processArgs(args);

            // Initialize API client connector
            UnmanagedHttpClient httpClient = new UnmanagedHttpClient(apiScheme,
                    apiHost, apiPort);
            httpClient.initialize();
            httpClient.setCommonHeaders(createStdHeaders());
            ICrossRefApiClient apiClient = new CrossRefApiHttpClient(httpClient);
            
            // Initialize matcher object
            ReferenceMatcher matcher = new ReferenceMatcher(apiClient);
            matcher.setCacheJournalAbbrevMap(true);
            matcher.initialize();
            
            // Get match results
            outputResults(matcher.match(request));
        } catch (IOException ex) {
            LOGGER.error("Error performing matching process: " + ex.getMessage(),
                    ex);
        }
    }
    
    /**
     * This method will attempt to load standard headers info from a local file.
     * The headers will be sent by the API client to the server when making 
     * HTTP calls.
     * 
     * @return A map of headers
     */
    private static Map<String, String> createStdHeaders() {
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
    
    /**
     * Initialize a matching request object from given input arguments as well
     * as other runtime variables. In the process, this method performs validation
     * and will exit the program if the validation fails.
     * 
     * @param args The arguments to process
     * 
     * @return Matching request
     */
    private static MatchRequest processArgs(String[] args) {
        // Define acceptable options
        Options options = new Options();
        options.addOption("it", "input-type", true,
            "Input type. Valid values are \"string\" and \"file\". This " +
            "option affects how the -i option is interpreted. If input type == " +
            "\"string\", the value of the -i option should be the input data " +
            "for the matching. If input type == \"file\", the value of the -i " +
            "option should be the path to the file from which the input data " +
            "will be read. In either case, the input is assumed to be in one " +
            "of two formats, either 1) a JSON Array of structured and/or " +
            "unstructured references, or 2) a delimited list of structured " +
            "and/or unstructured references. See also -d option.");
        options.addOption("i", "input", true,
            "The input data, or the path to the file containing the input " +
            "data, depending on the value of the -it option.");
        options.addOption("ct", "cand-min", true,
                "Candidate selection normalized threshold.");
        options.addOption("ut", "unstr-min", true,
                "Unstructured validation threshold.");
        options.addOption("st", "str-min", true,
                "Structured validation threshold.");
        options.addOption("ur", "unstr-rows", true,
                "Number of search items to consider as candidates in " +
                "unstructured matching.");
        options.addOption("sr", "str-rows", true,
                "Number of search items to consider as candidates in " +
                "structured matching.");
        options.addOption("as", "api-scheme", true,
                "CR API http scheme (http or https)");
        options.addOption("ah", "api-host", true, "CR API host");
        options.addOption("ap", "api-port", true, "CR API port");
        options.addOption("ak", "key-file", true, "CR API key file");
        options.addOption("d", "delim", true, "Textual data delimiter");
        options.addOption("o", "out-file", true, "Output file");
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
                    InputType.values()).stream()
                        .map(o-> {return o.getCode();})
                        .collect(Collectors.toList());
 
                throw new RuntimeException("Invalid input type specified: " + 
                        typeCode + ". Valid types are: " + okVals);
            }
            
            String inputValue = cmd.getOptionValue("i");
            if (inputType == InputType.FILE) {
                // Check for input file
                File file = new File(inputValue);
                if (!file.exists()) {
                    throw new RuntimeException(
                            "The specified input file does not exist: " + 
                            inputValue);
                }
            }
            
            // Init request with input type and value
            MatchRequest request = new MatchRequest(inputType, inputValue);
            
            /**
             * Optional request settings
             */
            
            if (cmd.hasOption("ct")) {
                request.setCandidateMinScore(
                        Double.valueOf(cmd.getOptionValue("ct")));
            }
            
            if (cmd.hasOption("ut")) {
                request.setUnstructuredMinScore(
                        Double.valueOf(cmd.getOptionValue("ut")));
            }
            
            if (cmd.hasOption("st")) {
                request.setStructuredMinScore(
                        Double.valueOf(cmd.getOptionValue("st")));
            }
            
            if (cmd.hasOption("sr")) {
                request.setStructuredRows(
                        Integer.valueOf(cmd.getOptionValue("sr")));
            }
            
            if (cmd.hasOption("ur")) {
                request.setUnstructuredRows(
                        Integer.valueOf(cmd.getOptionValue("ur")));
            }

            if (cmd.hasOption("d")) {
                request.setDataDelimiter(cmd.getOptionValue("d"));
            }
            
            /**
             * Optional process settings
             */
            
            if (cmd.hasOption("as")) {
               apiScheme = cmd.getOptionValue("as").toLowerCase();
               if (!(apiScheme.equals("http") || apiScheme.equals("https"))) {
                   throw new RuntimeException(
                           "Invalid http scheme: " + apiScheme);
               }
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

            // Return initialized request
            return request;
            
        } catch (RuntimeException | ParseException ex) {
            LOGGER.error("Error processing input arguments: " + ex.getMessage());
            printHelp(options);
            return null;
        }
    }
    
    /**
     * Display command line option help text.
     * 
     * @param options Available options
     */
    private static void printHelp(Options options) {
        System.out.println("\n");
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("MainApp [options]", options);     
        System.exit(0);
    }
    
    /**
     * Output results.
     * 
     * @param response The matching response
     */
    private static void outputResults(MatchResponse response) {
        JSONArray results = response.toJSON();
        
        if (outputFileName != null) {
            try {
                FileUtils.writeStringToFile(new File(outputFileName),
                        results.toString(2), "UTF-8");
            } catch (IOException ex) {
                LOGGER.error("Error writing output file: " + ex.getMessage(), ex);
            }
        } else {
            System.out.println(results.toString(2));
        }
    }
    
}