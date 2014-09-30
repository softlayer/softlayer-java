package com.softlayer.api.http;

import java.util.concurrent.ExecutorService;

/** Class implemented by HTTP client factories that use a Java thread pool */
public abstract class ThreadPooledHttpClientFactory extends HttpClientFactory {

    /**
     * By default the thread pool is a cached thread pool (using daemon threads) that is shutdown immediately
     * when it is overwritten by this method or the factory is finalized. Callers who supply a thread pool are
     * expected to handle its lifecycle. Null can be given here to revert to the default.
     * 
     * @param threadPool
     */
    public abstract void setThreadPool(ExecutorService threadPool);
}
