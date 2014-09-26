package com.softlayer.api.http;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public abstract class HttpClientFactory {
    
    static volatile HttpClientFactory defaultFactory = null;
    
    public static HttpClientFactory getDefault() {
        return getDefault(true);
    }

    static HttpClientFactory getDefault(boolean cache) {
        // We don't mind the race condition that can occur by possibly creating multiple factories. We make
        //  no guarantees that there is only one factory ever created even when cache is true
        HttpClientFactory result = cache ? defaultFactory : null;
        if (result == null) {
            Iterator<HttpClientFactory> iterator = ServiceLoader.load(HttpClientFactory.class).iterator();
            if (!iterator.hasNext()) {
                // Default to Gson (which may not be present, but we just let NoClassDefFoundError throw)
                result = new BuiltInHttpClientFactory();
            } else {
                result = iterator.next();
                if (iterator.hasNext()) {
                    throw new RuntimeException("Ambiguous HTTP client factories: " + result.getClass() +
                            ", " + iterator.next().getClass() + " and possibly more");
                }
            }
            if (cache) {
                defaultFactory = result;
            }
        }
        return result;
    }

    public abstract HttpClient getHttpClient(HttpCredentials credentials, String method,
            String fullUrl, Map<String, List<String>> headers);
}
