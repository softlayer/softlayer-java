package com.softlayer.api.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Interface representing an HTTP response from the HTTP client */
public interface HttpResponse {
    
    public int getStatusCode();
    
    public Map<String, List<String>> getHeaders();
    
    /** When this is used by the caller, he is expected to close it */
    public InputStream getInputStream();
}
