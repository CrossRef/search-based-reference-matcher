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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.crossref.common.rest.api.ICrossRefApiClient;
import org.crossref.common.rest.api.IHttpClient;
import org.crossref.common.rest.impl.CrossRefApiHttpClient;
import org.crossref.common.utils.LogUtils;
import org.crossref.common.utils.ManagedHttpClient;
import org.json.JSONObject;

/**
 * This class executes a matching request via a main application entrypoint.
 * 
 * @author joe.aparo
 */
public class MainApp {
    private final static String CRAPI_KEY_FILE = ".crapi_key";
    private static final Logger logger = LogUtils.getLogger();
    
    private static String apiScheme = ReferenceMatcher.DEFAULT_API_SCHEME;
    private static String apiHost = ReferenceMatcher.DEFAULT_API_HOST;
    private static int apiPort = ReferenceMatcher.DEFAULT_API_PORT;
    private static String apiKeyFile = System.getProperty("user.home") + "/" + CRAPI_KEY_FILE;
    
    public static void main(String[] args) {
        try {
            MatchRequest request = processArgs(args);

            // Initialize API client connector
            IHttpClient httpClient = new ManagedHttpClient(apiScheme, apiHost, apiPort, createStdHeaders());
            ICrossRefApiClient apiClient = new CrossRefApiHttpClient(httpClient);
            
            // Initialize matcher object
            ReferenceMatcher matcher = new ReferenceMatcher(apiClient);
            matcher.setCacheJournals(true);
            matcher.initialize();
            
            // Get match results
            MatchResponse response = matcher.match(request);
            
            // Display result
            System.out.println(new JSONObject(response.getMatches()));
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
        options.addOption("i", "inputtype", true,
            "input type; valid values are: \"refstr\" (a single unstructured "
            + "reference), \"txt\" (a file with unstructured reference "
            + "strings, one per line), \"json\" (JSON file with a list of "
            + "reference strings and/or structured references)");
        options.addOption("f", "file", true,
            "input file path, used with \"txt\" and \"json\" input types");
        options.addOption("s", "string", true,
            "reference string, used with \"refstr\" file type");
        options.addOption("ct", true, "candidate selection normalized threshold");
        options.addOption("ut", true, "unstructured validation threshold");
        options.addOption("st", true, "structured validation threshold");
        options.addOption("as", "api-scheme", true, "CR API http scheme (http or https)");
        options.addOption("ah", "api-host", true, "CR API host)");
        options.addOption("ap", "api-port", true, "CR API port");
        options.addOption("ak", "key-file", true, "CR API key file");
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
            
            // Check required input type
            if (!cmd.hasOption("i")) {
               throw new RuntimeException("Input type not given");
            }

            // Validate given input type
            String typeCode = cmd.getOptionValue("i");
            RequestInputType inputType = RequestInputType.getByCode(typeCode);
            if (inputType == null) {
                List<String> okVals = Arrays.asList(
                    RequestInputType.values()).stream().map(o-> {return o.getCode();}).collect(Collectors.toList());
 
                throw new RuntimeException("Invalid input type specified: " + typeCode + ". Valid types are: " + okVals);
            }
            
            // Validate input/output files
            String inputFile = null;
            if (inputType == RequestInputType.TEXT_FILE || inputType == RequestInputType.JSON_FILE) {
                if (!cmd.hasOption("f")) {
                    throw new RuntimeException("Input file path has to be provided for this input type.");
                }
             
                inputFile = cmd.getOptionValue("f");
                
                // Check existence of input file
                File file = new File(inputFile);
                if (!file.exists()) {
                    throw new RuntimeException("The specified input file does not exist: " + inputFile);
                }
            }

            // Unstructured string citation query
            String refString = null;
            if (inputType == RequestInputType.STRING) {
               if (!cmd.hasOption("s")) {
                   throw new RuntimeException("A reference string has to be provided for this input type.");
               }
               
               refString = cmd.getOptionValue("s");
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

            if (cmd.hasOption("ah")) {
               apiHost = cmd.getOptionValue("ah");
            }
            
            if (cmd.hasOption("ap")) {
               apiPort = Integer.valueOf(cmd.getOptionValue("ap"));
            } 
            
            if (cmd.hasOption("ak")) {
               apiKeyFile = cmd.getOptionValue("ak");
            } 
            
            // Return initialized request
            return new MatchRequest(
                inputType, candidateMinScore, unstructuredMinScore, 
                structuredMinScore, inputFile, refString);
            
        } catch (RuntimeException | ParseException ex) {
            logger.error("Error processing input arguments: " + ex.getMessage());
            printHelp(options);
            return null;
        }
    }
    
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
}
