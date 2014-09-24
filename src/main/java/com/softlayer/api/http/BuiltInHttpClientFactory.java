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
import java.util.concurrent.Future;

import javax.xml.bind.DatatypeConverter;

class BuiltInHttpClientFactory extends HttpClientFactory {

    @Override
    public HttpClient getHttpClient(HttpCredentials credentials, String method,
            String fullUrl, Map<String, List<String>> headers) {
        return new BuiltInHttpClient(credentials, method, fullUrl, headers);
    }

    static class BuiltInHttpClient implements HttpClient, HttpResponse {

        final HttpURLConnection connection;
        
        public BuiltInHttpClient(HttpCredentials credentials, String method,
                String fullUrl, Map<String, List<String>> headers) {
            // We only support basic auth
            if (credentials != null && !(credentials instanceof HttpBasicAuthCredentials)) {
                throw new UnsupportedOperationException("Only basic auth is supported, not " + credentials.getClass());
            }
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
            return this;
        }

        @Override
        public Future<HttpResponse> invokeAsync() {
            throw new UnsupportedOperationException("Asynchronous execution not supported");
        }

        @Override
        public void invokeAsync(Callable<Future<HttpResponse>> callback) {
            throw new UnsupportedOperationException("Asynchronous execution not supported");
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
