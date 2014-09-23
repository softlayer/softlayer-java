package com.softlayer.api.http;

public class HttpBasicAuthCredentials implements HttpCredentials {

    public final String username;
    public final String apiKey;
    
    public HttpBasicAuthCredentials(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
    }
}
