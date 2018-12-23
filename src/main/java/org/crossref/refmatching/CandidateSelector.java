package org.crossref.refmatching;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 *
 * @author Dominika Tkaczyk
 */
public class CandidateSelector {

    private final double minScore;
    private String authorization;
    private String mailto;
    
    private final static String CRAPI_KEY_FILE = ".crapi_key";
    private final static String CRAPI_URL = "https://api.crossref.org/";
    
    private final static int TIMEOUT = 30*1000;

    public CandidateSelector(double minScore) {
        this.minScore = minScore;
        try {
            String home = System.getProperty("user.home");
            String crapiData = FileUtils.readFileToString(
                    new File(home + "/" + CRAPI_KEY_FILE), "UTF-8");
            JSONObject crapiJson = new JSONObject(crapiData);
            authorization = crapiJson.optString("Authorization", null);
            mailto = crapiJson.optString("Mailto", null);
        } catch (IOException ex) {
        }
    }

    public List<Candidate> findCandidates(Reference reference) {
        try {
            if (reference.getString().isEmpty()) {
                return new ArrayList<>();
            }
            JSONArray candidates = search(reference.getString());
            return selectCandidates(reference.getString(), candidates);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CandidateSelector.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CandidateSelector.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return new ArrayList<>();
    }

    private JSONArray search(String refString)
            throws MalformedURLException, UnsupportedEncodingException,
            IOException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(TIMEOUT);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(TIMEOUT);

        HttpClientBuilder builder = HttpClientBuilder.create();     
        builder.setDefaultRequestConfig(requestBuilder.build());
        HttpClient httpclient = builder.build();
        HttpGet httpget = new HttpGet(
                CRAPI_URL + "/works?query.bibliographic="
                + URLEncoder.encode(refString, "UTF-8"));
        if (authorization != null) {
            httpget.setHeader("Authorization", authorization);
        }
        if (mailto != null) {
            httpget.setHeader("Mailto", mailto);
        }
        HttpResponse response = httpclient.execute(httpget);
        response.getEntity().getContent();
        try {
	    JSONObject json = new JSONObject(IOUtils.toString(
                response.getEntity().getContent(), "UTF-8"));
            return json.getJSONObject("message").optJSONArray("items");
	} catch (JSONException e) {
	    return new JSONArray();
	}
    }

    private List<Candidate> selectCandidates(String refString, JSONArray items) {
        List<Candidate> candidates = new ArrayList<>();
	if (items == null) {
	    return candidates;
	}
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (candidates.isEmpty()) {
                candidates.add(new Candidate(item));
            } else if (item.getDouble("score") / refString.length() >= minScore) {
                candidates.add(new Candidate(item));
            } else {
                break;
            }
        }
        return candidates;
    }

}
