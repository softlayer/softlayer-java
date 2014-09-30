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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.DatatypeConverter;

import com.softlayer.api.ResponseHandler;

/**
 * Default implementation of {@link HttpClientFactory} that only supports simple {@link HttpURLConnection}.
 */
class BuiltInHttpClientFactory extends ThreadPooledHttpClientFactory {

    // Volatile is not enough here, we have to have more control over setting and what not
    ExecutorService threadPool;
    boolean threadPoolUserDefined;
    final ReadWriteLock threadPoolLock = new ReentrantReadWriteLock();
    
    @Override
    public BuiltInHttpClient getHttpClient(HttpCredentials credentials, String method,
            String fullUrl, Map<String, List<String>> headers) {
        return new BuiltInHttpClient(credentials, method, fullUrl, headers);
    }
    
    public ExecutorService getThreadPool() {
        // We support lazy loading in this method and we guarantee it's thread safe, but we do not
        //  synchronize on it to prevent lock down on the entire class during lots of contention
        //  especially since the lazy-loading is expected to be rare.
        threadPoolLock.readLock().lock();
        try {
            if (threadPool != null) {
                return threadPool;
            }
        } finally {
            threadPoolLock.readLock().unlock();
        }
        threadPoolLock.writeLock().lock();
        try {
            if (threadPool == null) {
                // Here, we want to use a cached thread pool by default, but we need a custom thread
                //  factory to make the threads daemon threads. This defalt can be overridden by users,
                //  but in general we do not want the API client to hold a process open by default.
                threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
                    final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = defaultFactory.newThread(r);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
                threadPoolUserDefined = false;
            }
            return threadPool;
        } finally {
            threadPoolLock.writeLock().unlock();
        }
    }
    
    @Override
    public void setThreadPool(ExecutorService threadPool) {
        threadPoolLock.writeLock().lock();
        try {
            // Shutdown existing one if a new one is being given and the existing
            //  one is the default. Otherwise, if there was an existing one and it
            //  was supplied by the user, it's his responsibility to shut it down.
            if (this.threadPool != null && !threadPoolUserDefined) {
                this.threadPool.shutdownNow();
            }
            this.threadPool = threadPool;
            threadPoolUserDefined = threadPool != null;
        } finally {
            threadPoolLock.writeLock().unlock();
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
        
        void openConnection() {
            try {
                connection = (HttpURLConnection) new URL(fullUrl).openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public HttpResponse invokeSync(Callable<?> setupBody) {
            // We let HTTP URL connection do it's invocation when it wants. The built-in HTTP connection
            //  usually starts a stream when the request method is set or when the output or response code
            //  is requested. It switches from send to receive when the output or response code is requested.
            //  Its resources are closed when the send stream (if used) and receive stream (if used) are
            //  closed and internally the JVM is allowed to pool connections to common hosts which makes this
            //  fairly fast and safe.
            openConnection();
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
            try {
                setupBody.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        @Override
        public Future<HttpResponse> invokeAsync(final Callable<?> setupBody) {
            return getThreadPool().submit(new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() throws Exception {
                    // We let any exception here properly bubble out of the future
                    HttpResponse response = invokeSync(setupBody);
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
                        resp = invokeSync(setupBody);
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
                // Asking for the input stream on non-success will fail
                if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
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
