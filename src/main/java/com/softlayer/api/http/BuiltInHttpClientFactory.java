package com.softlayer.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.DatatypeConverter;

import com.softlayer.api.ResponseHandler;

/**
 * Default implementation of {@link HttpClientFactory} that only supports simple {@link HttpURLConnection}.
 */
class BuiltInHttpClientFactory extends ThreadPooledHttpClientFactory {

    // Volatile is not enough here, we have to have more control over setting and what not
    private ExecutorService threadPool;
    private boolean threadPoolUserDefined;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    @Override
    public HttpClient getHttpClient(HttpCredentials credentials, String method,
            String fullUrl, Map<String, List<String>> headers) {
        return new BuiltInHttpClient(credentials, method, fullUrl, headers);
    }
    
    public ExecutorService getThreadPool() {
        lock.readLock().lock();
        try {
            if (threadPool != null) {
                return threadPool;
            }
        } finally {
            lock.readLock().unlock();
        }
        lock.writeLock().lock();
        try {
            if (threadPool == null) {
                threadPool = Executors.newCachedThreadPool();
                threadPoolUserDefined = false;
            }
            return threadPool;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void setThreadPool(ExecutorService threadPool) {
        lock.writeLock().lock();
        try {
            // Shutdown existing one if a new one is being given
            if (this.threadPool != null && !threadPoolUserDefined) {
                this.threadPool.shutdownNow();
            }
            this.threadPool = threadPool;
            threadPoolUserDefined = threadPool != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    class BuiltInHttpClient implements HttpClient, HttpResponse {

        final HttpBasicAuthCredentials credentials;
        final String method;
        final String fullUrl;
        final Map<String, List<String>> headers;
        HttpURLConnection connection;
        
        public BuiltInHttpClient(HttpCredentials credentials, String method,
                String fullUrl, Map<String, List<String>> headers) {
            // We only support basic auth
            if (credentials != null && !(credentials instanceof HttpBasicAuthCredentials)) {
                throw new UnsupportedOperationException("Only basic auth is supported, not " + credentials.getClass());
            }
            this.credentials = (HttpBasicAuthCredentials) credentials;
            this.method = method;
            this.fullUrl = fullUrl;
            this.headers = headers;
        }
        
        @Override
        public OutputStream getBodyStream() {
            try {
                connection.setDoOutput(true);
                return connection.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public HttpResponse invokeSync() {
            // We let HTTP URL connection do it's invocation when it wants
            try {
                connection = (HttpURLConnection) new URL(fullUrl).openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (credentials != null) {
                // XXX: Using JAXB datatype converter here because it's the only base 64 I trust to be around...
                //  should we embed a base 64 encoder in here?
                HttpBasicAuthCredentials authCredentials = (HttpBasicAuthCredentials) credentials;
                try {
                    connection.addRequestProperty("Authorization", "Basic " + new String(DatatypeConverter.
                            printBase64Binary((authCredentials.username + ':' + authCredentials.apiKey).getBytes("UTF-8"))));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
                for (String headerValue : headerEntry.getValue()) {
                    connection.addRequestProperty(headerEntry.getKey(), headerValue);
                }
            }
            if (!"GET".equals(method)) {
                try {
                    connection.setRequestMethod(method);
                } catch (ProtocolException e) {
                    throw new RuntimeException(e);
                }
            }
            return this;
        }

        @Override
        public Future<HttpResponse> invokeAsync(final Callable<?> setupBody) {
            return getThreadPool().submit(new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() throws Exception {
                    HttpResponse response = invokeSync();
                    setupBody.call();
                    return response;
                }
            });
        }

        @Override
        public Future<?> invokeAsync(final Callable<?> setupBody, final ResponseHandler<HttpResponse> callback) {
            return getThreadPool().submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    HttpResponse resp;
                    try {
                        resp = invokeSync();
                        setupBody.call();
                    } catch (Exception e) {
                        callback.onError(e);
                        return null;
                    }
                    callback.onSuccess(resp);
                    return null;
                }
            });
        }

        @Override
        public void close() throws IOException {
            // Nothing to do, callers are expected to close streams they use
        }

        @Override
        public int getStatusCode() {
            try {
                return connection.getResponseCode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return connection.getHeaderFields();
        }

        @Override
        public InputStream getInputStream() {
            try {
                if (connection.getResponseCode() == 200) {
                    return connection.getInputStream();
                } else {
                    return connection.getErrorStream();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
