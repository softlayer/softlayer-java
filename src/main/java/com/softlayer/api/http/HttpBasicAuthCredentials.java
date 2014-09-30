package com.softlayer.api.http;

/** HTTP basic authentication support for username and API key */
public class HttpBasicAuthCredentials implements HttpCredentials {

    public final String username;
    public final String apiKey;
    
    public HttpBasicAuthCredentials(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
    }
}
