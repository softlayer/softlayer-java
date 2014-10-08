package com.softlayer.api.http;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Base class for an HTTP client factory. By default the {@link BuiltInHttpClientFactory} is used. This
 * can be overridden by a custom client factory using the {@link ServiceLoader} pattern. It is also
 * cached (though not necessarily guaranteed to be a singleton or instantiated only once) upon first access.
 */
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
                // Default to built-in version
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

    /**
     * Get the HTTP client for the given request information. The resulting client is only used once.
     */
    public abstract HttpClient getHttpClient(HttpCredentials credentials, String method,
            String fullUrl, Map<String, List<String>> headers);
}
