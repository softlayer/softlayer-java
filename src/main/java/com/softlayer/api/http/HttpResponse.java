package com.softlayer.api.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Interface representing an HTTP response from the HTTP client */
public interface HttpResponse {
    
    int getStatusCode();
    
    Map<String, List<String>> getHeaders();
    
    /** When this is used by the caller, he is expected to close it */
    InputStream getInputStream();
}
