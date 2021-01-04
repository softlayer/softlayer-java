package com.softlayer.api.http;

/** HTTP bearer authorization support for bearer token fromhttps://iam.cloud.ibm.com/identity/token */
public class HttpBearerCredentials implements HttpCredentials {

    public final String token;
    
    public HttpBearerCredentials(String token) {
        this.token = token;
    }

    /**
     * Formats the token into a HTTP Authorization header.
     *
     * @return String
     */
    public String getHeader() {
        return "Bearer " + token;
    }
}
