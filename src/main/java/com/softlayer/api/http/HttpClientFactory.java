package com.softlayer.api.http;

import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class HttpClientFactory {

    public static HttpClientFactory getDefault() {
        Iterator<HttpClientFactory> iterator = ServiceLoader.load(HttpClientFactory.class).iterator();
        if (!iterator.hasNext()) {
            return new BuiltInHttpClientFactory();
        }
        HttpClientFactory factory = iterator.next();
        if (iterator.hasNext()) {
            throw new RuntimeException("Ambiguous HTTP client factories: " + factory.getClass() +
                    ", " + iterator.next().getClass() + " and possibly more");
        }
        return factory;
    }
}
