package com.softlayer.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.softlayer.api.annotation.ApiMethod;
import com.softlayer.api.annotation.ApiService;
import com.softlayer.api.http.HttpBasicAuthCredentials;
import com.softlayer.api.http.HttpClient;
import com.softlayer.api.http.HttpClientFactory;
import com.softlayer.api.http.HttpResponse;
import com.softlayer.api.json.JsonMarshallerFactory;
import com.softlayer.api.service.Entity;

/** Implementation of API client for http://sldn.softlayer.com/article/REST */
public class RestApiClient implements ApiClient {

    public static final String BASE_URL = "https://api.softlayer.com/rest/v3.1/";
    
    static final String BASE_PKG = Entity.class.getPackage().getName();
    
    static final Map<String, List<String>> HEADERS;
    
    static {
        HEADERS = Collections.singletonMap("X-SoftLayer-Include-API-Types", Collections.singletonList("true"));
    }
    
    private final String baseUrl;
    private HttpClientFactory httpClientFactory;
    private JsonMarshallerFactory jsonMarshallerFactory;
    private boolean loggingEnabled = false;
    private HttpBasicAuthCredentials credentials;
    
    public RestApiClient() {
        this(BASE_URL);
    }
    
    public RestApiClient(String baseUrl) {
        // Add trailing slash if not present
        if (!baseUrl.endsWith("/")) {
            baseUrl += '/';
        }
        this.baseUrl = baseUrl;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public HttpClientFactory getHttpClientFactory() {
        if (httpClientFactory == null) {
            httpClientFactory = HttpClientFactory.getDefault();
        }
        return httpClientFactory;
    }
    
    public void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }
    
    public RestApiClient withHttpClientFactory(HttpClientFactory httpClientFactory) {
        setHttpClientFactory(httpClientFactory);
        return this;
    }
    
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
    
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }
    
    public RestApiClient withLoggingEnabled() {
        this.loggingEnabled = true;
        return this;
    }
    
    public JsonMarshallerFactory getJsonMarshallerFactory() {
        if (jsonMarshallerFactory == null) {
            jsonMarshallerFactory = JsonMarshallerFactory.getDefault();
        }
        return jsonMarshallerFactory;
    }
    
    public void setJsonMarshallerFactory(JsonMarshallerFactory jsonMarshallerFactory) {
        this.jsonMarshallerFactory = jsonMarshallerFactory;
    }
    
    public RestApiClient withJsonMarshallerFactory(JsonMarshallerFactory jsonMarshallerFactory) {
        setJsonMarshallerFactory(jsonMarshallerFactory);
        return this;
    }
    
    @Override
    public RestApiClient withCredentials(String username, String apiKey) {
        credentials = new HttpBasicAuthCredentials(username, apiKey);
        return this;
    }
    
    public HttpBasicAuthCredentials getCredentials() {
        return credentials;
    }
    
    protected void writeParameterHttpBody(Object[] params, OutputStream out) {
        getJsonMarshallerFactory().getJsonMarshaller().toJson(
                Collections.singletonMap("parameters", params), out);
    }
    
    protected String getHttpMethodFromMethodName(String methodName) {
        if ("deleteObject".equals(methodName)) {
            return "DELETE";
        } else if ("createObject".equals(methodName) || "createObjects".equals(methodName)) {
            return "POST";
        } else if ("editObject".equals(methodName) || "editObjects".equals(methodName)) {
            return "PUT";
        } else {
            return "GET";
        }
    }
    
    protected String getFullUrl(String serviceName, String methodName, Long id,
            ResultLimit resultLimit, String maskString) {
        StringBuilder url = new StringBuilder(baseUrl + serviceName);
        // ID present? add it
        if (id != null) {
            url.append('/').append(id);
        }
        // Some method names are not included, others can have the "get" stripped
        if (methodName.startsWith("get") && !"getObject".equals(methodName)) {
            url.append('/').append(methodName.substring(3));
        } else if (!"getObject".equals(methodName) && !"deleteObject".equals(methodName) &&
                !"createObject".equals(methodName) && !"createObjects".equals(methodName) &&
                !"editObject".equals(methodName) && !"editObjects".equals(methodName)) {
            url.append('/').append(methodName);
        }
        url.append(".json");
        if (resultLimit != null) {
            url.append("?resultLimit=").append(resultLimit.offset).append(',').append(resultLimit.limit);
        }
        if (maskString != null && !maskString.isEmpty()) {
            url.append(resultLimit == null ? '?' : '&');
            try {
                url.append("objectMask=").append(URLEncoder.encode(maskString, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return url.toString();
    }
    
    protected void logRequest(String httpMethod, String url, Object[] params) {
        // Build JSON
        String body = "no body";
        if (params != null && params.length > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getJsonMarshallerFactory().getJsonMarshaller().toJson(
                    Collections.singletonMap("parameters", params), out);
            try {
                body = "body: " + out.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.format("Running %s on %s with %s\n", httpMethod, url, body);
    }
    
    protected void logResponse(String url, int statusCode, String body) {
        System.out.format("Got %d on %s with body: %s\n", statusCode, url, body);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Service> S createService(Class<S> serviceClass, Long id) {
        return (S) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { serviceClass }, new ServiceProxy<S>(serviceClass, id));
    }

    class ServiceProxy<S extends Service> implements InvocationHandler {
        
        final Class<S> serviceClass;
        final Long id;
        Mask mask;
        String maskString;
        ResultLimit resultLimit;
        Integer lastResponseTotalItemCount;
        
        public ServiceProxy(Class<S> serviceClass, Long id) {
            this.serviceClass = serviceClass;
            this.id = id;
        }
        
        public void logRequestAndWriteBody(HttpClient client, String httpMethod, String url, Object[] args) {
            if (loggingEnabled) {
                logRequest(httpMethod, url, args);
            }
            // If there are parameters write em
            if (args != null && args.length > 0) {
                OutputStream outStream = client.getBodyStream();
                try {
                    writeParameterHttpBody(args, outStream);
                } finally {
                    try { outStream.close(); } catch (Exception e) { }
                }
            }
        }
        
        @SuppressWarnings("resource")
        public Object logAndHandleResponse(HttpResponse response, String url,
                java.lang.reflect.Type returnType) throws Exception {
            InputStream stream = response.getInputStream();
            if (loggingEnabled && stream != null) {
                InputStream newStream;
                Scanner scanner = null;
                try {
                    scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
                    String body = scanner.hasNext() ? scanner.next() : "";
                    logResponse(url, response.getStatusCode(), body);
                    newStream = new ByteArrayInputStream(body.getBytes("UTF-8"));
                } finally {
                    try {
                        if (scanner != null) {
                            scanner.close();
                        }
                    } catch (Exception e) { }
                    try {
                        stream.close();
                    } catch (Exception e) { }
                }
                stream = newStream;
            }
            try {
                // If it's not a 200, we have a problem
                if (response.getStatusCode() < 200 || response.getStatusCode() > 300) {
                    if (stream == null) {
                        throw new ApiException("Unknown error", null, response.getStatusCode());
                    }
                    // Extract error and throw
                    Map<String, String> map = getJsonMarshallerFactory().getJsonMarshaller().
                            fromJson(Map.class, stream);
                    throw ApiException.fromError(map.get("error"), map.get("code"), response.getStatusCode());
                }
                // Update total items
                lastResponseTotalItemCount = null;
                Map<String, List<String>> headers = response.getHeaders();
                if (headers != null) {
                    List<String> totalItems = headers.get("X-SoftLayer-Total-Items");
                    if (totalItems != null && !totalItems.isEmpty()) {
                        lastResponseTotalItemCount = Integer.valueOf(totalItems.get(0));
                    }
                }
                // Just return the serialized response
                return getJsonMarshallerFactory().getJsonMarshaller().fromJson(returnType, stream);
            } finally {
                try { stream.close(); } catch (Exception e) { }
            }
        }
        
        public Object invokeService(Method method, final Object[] args) throws Throwable {
            ApiMethod methodInfo = method.getAnnotation(ApiMethod.class);
            // Must have ID if instance is required
            if (methodInfo.instanceRequired() && id == null) {
                throw new IllegalStateException("ID is required to invoke " + method);
            }
            String methodName = methodInfo.value().isEmpty() ? method.getName() : methodInfo.value();
            final String httpMethod = getHttpMethodFromMethodName(methodName);
            Long methodId = methodInfo.instanceRequired() ? this.id : null;
            final String url = getFullUrl(serviceClass.getAnnotation(ApiService.class).value(),
                    methodName, methodId, resultLimit, mask == null ? maskString : mask.getMask());
            final HttpClient client = getHttpClientFactory().getHttpClient(credentials, httpMethod, url, HEADERS);

            // Invoke with response
            HttpResponse response = client.invokeSync(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    logRequestAndWriteBody(client, httpMethod, url, args);
                    return null;
                }
            });
            
            return logAndHandleResponse(response, url, method.getGenericReturnType());
        }
        
        @SuppressWarnings("unchecked")
        public Object invokeServiceAsync(final Method asyncMethod, final Object[] args) throws Throwable {
            // If the last parameter is a callback, it is a different type of invocation
            Class<?>[] parameterTypes = asyncMethod.getParameterTypes();
            boolean lastParamCallback = parameterTypes.length > 0 &&
                ResponseHandler.class.isAssignableFrom(parameterTypes[parameterTypes.length - 1]);
            final Object[] trimmedArgs;
            if (lastParamCallback) {
                parameterTypes = Arrays.copyOfRange(parameterTypes, 0, parameterTypes.length - 1);
                trimmedArgs = Arrays.copyOfRange(args, 0, args.length - 1);
            } else {
                trimmedArgs = args;
            }
            
            final Method method = serviceClass.getMethod(asyncMethod.getName(), parameterTypes);
            ApiMethod methodInfo = method.getAnnotation(ApiMethod.class);
            // Must have ID if instance is required
            if (methodInfo.instanceRequired() && id == null) {
                throw new IllegalStateException("ID is required to invoke " + method);
            }
            String methodName = methodInfo.value().isEmpty() ? method.getName() : methodInfo.value();
            final String httpMethod = getHttpMethodFromMethodName(methodName);
            Long methodId = methodInfo.instanceRequired() ? this.id : null;
            final String url = getFullUrl(serviceClass.getAnnotation(ApiService.class).value(),
                    methodName, methodId, resultLimit, mask == null ? maskString : mask.getMask());
            final HttpClient client = getHttpClientFactory().getHttpClient(credentials, httpMethod, url, HEADERS);
            
            Callable<Void> setupBody = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    logRequestAndWriteBody(client, httpMethod, url, trimmedArgs);
                    return null;
                }
            };
            
            if (lastParamCallback) {
                final ResponseHandler<Object> handler = (ResponseHandler<Object>) args[args.length - 1];
                return client.invokeAsync(setupBody, new ResponseHandler<HttpResponse>() {
                    @Override
                    public void onSuccess(HttpResponse value) {
                        Object result;
                        try {
                            result = logAndHandleResponse(value, url, method.getGenericReturnType());
                        } catch (Exception e) {
                            onError(e);
                            return;
                        }
                        if (handler != null) {
                            if (handler instanceof ResponseHandlerWithHeaders) {
                                ((ResponseHandlerWithHeaders<?>) handler).setLastResponseTotalItemCount(
                                    lastResponseTotalItemCount);
                            }
                            handler.onSuccess(result);
                        }
                    }
                    
                    @Override
                    public void onError(Exception ex) {
                        if (handler != null) {
                            handler.onError(ex);
                        }
                    }
                });
            } else {
                final Future<HttpResponse> future = client.invokeAsync(setupBody);
                return new Future<Object>() {
                    private boolean responseAttempted;
                    private Object response;

                    @Override
                    public boolean cancel(boolean mayInterruptIfRunning) {
                        return future.cancel(mayInterruptIfRunning);
                    }

                    @Override
                    public synchronized Object get() throws InterruptedException, ExecutionException {
                        if (!responseAttempted) {
                            responseAttempted = true;
                            try {
                                response = logAndHandleResponse(future.get(), url, method.getGenericReturnType());
                            } catch (Exception e) {
                                throw new ExecutionException(e);
                            }
                        }
                        return response;
                    }

                    @Override
                    public synchronized Object get(long timeout, TimeUnit unit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        if (!responseAttempted) {
                            responseAttempted = true;
                            try {
                                response = logAndHandleResponse(future.get(timeout, unit),
                                    url, method.getGenericReturnType());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return response;
                    }

                    @Override
                    public boolean isCancelled() {
                        return future.isCancelled();
                    }

                    @Override
                    public boolean isDone() {
                        return future.isDone();
                    }
                };
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean noParams = args == null || args.length == 0;
            if ("asAsync".equals(method.getName()) && noParams) {
                ServiceProxy<S> asyncProxy = new ServiceProxy<S>(serviceClass, id);
                asyncProxy.mask = mask;
                asyncProxy.maskString = maskString;
                asyncProxy.resultLimit = resultLimit;
                return Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] { method.getReturnType() }, asyncProxy);
            } else if ("withNewMask".equals(method.getName()) && noParams) {
                mask = (Mask) method.getReturnType().newInstance();
                maskString = null;
                return mask;
            } else if ("withMask".equals(method.getName()) && noParams) {
                if (mask == null) {
                    mask = (Mask) method.getReturnType().newInstance();
                    maskString = null;
                }
                return mask;
            } else if ("setMask".equals(method.getName()) && args != null
                    && args.length == 1 && args[0] instanceof String) {
                mask = null;
                maskString = args[0].toString();
                return null;
            } else if ("setMask".equals(method.getName()) && args != null
                    && args.length == 1 && args[0] instanceof Mask) {
                mask = (Mask) args[0];
                maskString = null;
                return null;
            } else if ("setResultLimit".equals(method.getName()) &&
                    method.getDeclaringClass() == ResultLimitable.class) {
                resultLimit = (ResultLimit) args[0];
                return null;
            } else if ("getResultLimit".equals(method.getName()) &&
                    method.getDeclaringClass() == ResultLimitable.class) {
                return resultLimit;
            } else if ("getLastResponseTotalItemCount".equals(method.getName()) &&
                    method.getDeclaringClass() == ResultLimitable.class) {
                return lastResponseTotalItemCount;
            } else if (Service.class.isAssignableFrom(method.getDeclaringClass())) {
                return invokeService(method, args);
            } else if (ServiceAsync.class.isAssignableFrom(method.getDeclaringClass())) {
                return invokeServiceAsync(method, args);
            } else {
                // Should not be possible
                throw new RuntimeException("Unrecognized method: " + method);
            }
        }
    }
}
