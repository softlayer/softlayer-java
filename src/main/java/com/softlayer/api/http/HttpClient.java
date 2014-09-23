package com.softlayer.api.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface HttpClient extends Closeable {

    /** Stream to write body contents to (if at all). */
    public OutputStream getBodyStream();
    
    /** Make synchronous HTTP invocation. Throws if unable to connect. Errors from the API are returned normally. */
    public HttpResponse invokeSync();
    
    public Future<HttpResponse> invokeAsync();
    public void invokeAsync(Callable<Future<HttpResponse>> callback);
    
    /**
     * {@inheritDoc}
     *
     * When this is called, this HTTP client (and anything gotten from it) will not be used anymore.
     */
    public void close() throws IOException;
}
