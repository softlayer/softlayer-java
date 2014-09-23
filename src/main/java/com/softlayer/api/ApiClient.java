package com.softlayer.api;

public interface ApiClient {
    public ApiClient withCredentials(String username, String apiKey);
    
    public <M extends Mask, A extends ServiceAsync<M>, S extends Service<M, A>> S createService(
            Class<S> serviceClass, Class<A> asyncClass, Class<M> maskClass, Long id);
}
