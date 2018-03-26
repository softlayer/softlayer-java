package com.softlayer.api.http;

import java.util.Base64;

/** HTTP basic authorization support for username and API key */
public class HttpBasicAuthCredentials implements HttpCredentials {

    public final String username;
    public final String apiKey;
    
    public HttpBasicAuthCredentials(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
    }

    /**
     * Gets the encoded representation of the basic authentication credentials
     * for use in an HTTP Authorization header.
     *
     * @return String
     */
    public String getHeader() {
        String authPair = username + ':' + apiKey;
        return "Basic " + Base64.getEncoder().encodeToString(authPair.getBytes());
    }
}
