package com.softlayer.api.http;

import java.io.UnsupportedEncodingException;
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
     * @throws UnsupportedEncodingException If encoding with UTF-8 fails.
     */
    public String getHeader() throws UnsupportedEncodingException
    {
        String authPair = username + ':' + apiKey;
        String result = "Basic ";
        result += new String(
            Base64.getEncoder().encode(authPair.getBytes("UTF-8")),
            "UTF-8"
        );
        return result;
    }
}
