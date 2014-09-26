package com.softlayer.api;

import com.softlayer.api.temp.Account;

/** Common interface for all API clients. {@link RestApiClient} is the preferred implementation */
public interface ApiClient {
    
    /**
     * Set the username and API key credentials. This is required for most service methods.
     *
     * @return This instance
     */
    public ApiClient withCredentials(String username, String apiKey);
    
    /**
     * Get a service for the given sets of classes and optional ID. It is not recommended to call this
     * directly, but rather invoke the service method on the type class. E.g. {@link Account#service(ApiClient)}
     */
    public <S extends Service> S createService(Class<S> serviceClass, Long id);
}
