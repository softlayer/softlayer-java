package com.softlayer.api.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.softlayer.api.ResponseHandler;

public class FakeHttpClientFactory extends HttpClientFactory implements HttpClient, HttpResponse {
    
    public final int statusCode;
    public final Map<String, List<String>> responseHeaders;
    public final String responseBody;
    
    public HttpCredentials credentials;
    public String method;
    public String fullUrl;
    public Map<String, List<String>> headers;
    public boolean closeCalled;
    public ByteArrayOutputStream outStream;
    public boolean invokeSyncCalled;
    public boolean invokeAsyncFutureCalled;
    public boolean invokeAsyncCallbackCalled;
    
    public FakeHttpClientFactory(int statusCode, Map<String, List<String>> responseHeaders, String responseBody) {
        this.statusCode = statusCode;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return responseHeaders;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new ByteArrayInputStream(responseBody.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public HttpClient getHttpClient(HttpCredentials credentials,
            String method, String fullUrl, Map<String, List<String>> headers) {
        this.credentials = credentials;
        this.method = method;
        this.fullUrl = fullUrl;
        this.headers = headers;
        return this;
    }

    @Override
    public void close() throws IOException {
        closeCalled = true;
    }

    @Override
    public OutputStream getBodyStream() {
        outStream = new ByteArrayOutputStream();
        return outStream;
    }

    @Override
    public HttpResponse invokeSync(Callable<?> setupBody) {
        invokeSyncCalled = true;
//        try {
//            setupBody.call();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        return this;
    }

    @Override
    public Future<HttpResponse> invokeAsync(final Callable<?> setupBody) {
        invokeAsyncFutureCalled = true;
        return new Future<HttpResponse>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isCancelled() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDone() {
                throw new UnsupportedOperationException();
            }

            @Override
            public HttpResponse get() throws InterruptedException, ExecutionException {
                try {
                    setupBody.call();
                    return FakeHttpClientFactory.this;
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }

            @Override
            public HttpResponse get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                try {
                    setupBody.call();
                    return FakeHttpClientFactory.this;
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }
        };
    }

    @Override
    public Future<?> invokeAsync(final Callable<?> setupBody, final ResponseHandler<HttpResponse> callback) {
        invokeAsyncCallbackCalled = true;
        return new Future<Void>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isCancelled() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDone() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Void get() throws InterruptedException, ExecutionException {
                try {
                    setupBody.call();
                } catch (Exception e) {
                    callback.onError(e);
                    return null;
                }
                callback.onSuccess(FakeHttpClientFactory.this);
                return null;
            }

            @Override
            public Void get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                try {
                    setupBody.call();
                } catch (Exception e) {
                    callback.onError(e);
                    return null;
                }
                callback.onSuccess(FakeHttpClientFactory.this);
                return null;
            }
        };
    }
}
