package com.softlayer.api.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface HttpResponse {
    public int getStatusCode();
    public Map<String, List<String>> getHeaders();
    public InputStream getInputStream();
}
