package com.softlayer.api.http;

import java.io.Closeable;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.softlayer.api.ResponseHandler;

/**
 * This class is not thread-safe even when using invoke async. This class will only live for the
 * duration of one HTTP request.
 */
public interface HttpClient extends Closeable {

    /** Stream to write body contents to (if at all). When called, callers are expected to close it. */
    public OutputStream getBodyStream();
    
    /** Make synchronous HTTP invocation. Throws if unable to connect. Errors from the API are returned normally. */
    public HttpResponse invokeSync(Callable<?> setupBody);
    
    /** Make asynchronous HTTP invocation. All errors (inability to connect or API errors) are in the future. */
    public Future<HttpResponse> invokeAsync(Callable<?> setupBody);
    
    /** Callback-form of {@link #invokeAsync(Callable)} */
    public Future<?> invokeAsync(Callable<?> setupBody, ResponseHandler<HttpResponse> callback);
}
