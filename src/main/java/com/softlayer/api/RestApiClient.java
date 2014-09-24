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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.softlayer.api.annotation.ApiMethod;
import com.softlayer.api.annotation.ApiService;
import com.softlayer.api.http.HttpBasicAuthCredentials;
import com.softlayer.api.http.HttpClient;
import com.softlayer.api.http.HttpClientFactory;
import com.softlayer.api.http.HttpResponse;
import com.softlayer.api.json.JsonMarshallerFactory;
import com.softlayer.api.temp.Entity;

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
    
    protected String getFullUrl(String serviceName, String methodName, Long id, String maskString) {
        StringBuilder url = new StringBuilder(baseUrl + serviceName);
        // ID present? add it
        if (id != null) {
            url.append('/').append(id);
        }
        // Some method names are not included, others can have the "get" stripped
        if (methodName.startsWith("get")) {
            url.append('/').append(methodName.substring(3));
        } else if (!"getObject".equals(methodName) && !"deleteObject".equals(methodName) &&
                !"createObject".equals(methodName) && !"createObjects".equals(methodName) &&
                !"editObject".equals(methodName) && !"editObjects".equals(methodName)) {
            url.append('/').append(methodName);
        }
        url.append(".json");
        if (maskString != null && !maskString.isEmpty()) {
            try {
                url.append("?objectMask=").append(URLEncoder.encode(maskString, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return url.toString();
    }
    
    protected void logRequest(String httpMethod, String url, Object[] params) {
        // Build JSON
        String body = "";
        if (params != null && params.length > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getJsonMarshallerFactory().getJsonMarshaller().toJson(
                    Collections.singletonMap("parameters", params), out);
            try {
                body = out.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.format("Running %s on %s with body: %s\n", httpMethod, url, body);
    }
    
    protected void logResponse(String url, int statusCode, String body) {
        System.out.format("Got %d on %s with body: %s\n", statusCode, url, body);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <M extends Mask, A extends ServiceAsync<M>, S extends Service<M, A>> S createService(
            Class<S> serviceClass, Class<A> asyncClass, Class<M> maskClass, Long id) {
        return (S) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { serviceClass }, new ServiceProxy<M, A, S>(serviceClass, asyncClass, maskClass, id));
    }

    class ServiceProxy<M extends Mask, A extends ServiceAsync<M>, S extends Service<M, A>>
            implements InvocationHandler {
        
        final Class<S> serviceClass;
        final Class<A> asyncClass;
        final Class<M> maskClass;
        final Long id;
        M mask;
        String maskString;
        
        public ServiceProxy(Class<S> serviceClass, Class<A> asyncClass, Class<M> maskClass, Long id) {
            this.serviceClass = serviceClass;
            this.asyncClass = asyncClass;
            this.maskClass = maskClass;
            this.id = id;
        }
        
        @SuppressWarnings("unchecked")
        protected A asAsync() {
            ServiceProxy<M, A, S> proxy = new ServiceProxy<M, A, S>(serviceClass, asyncClass, maskClass, id);
            proxy.mask = mask;
            proxy.maskString = maskString;
            return (A) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { asyncClass }, proxy);
        }
        
        @SuppressWarnings("unchecked")
        protected Object maskInvocation(Method method, Object argument) throws Exception {
            if ("withNewMask".equals(method.getName())) {
                mask = maskClass.newInstance();
                maskString = null;
                return mask;
            } else if ("withMask".equals(method.getName())) {
                if (mask == null) {
                    mask = maskClass.newInstance();
                    maskString = null;
                }
                return mask;
            } else if ("setMask".equals(method.getName()) && argument instanceof String) {
                mask = null;
                maskString = argument.toString();
                return null;
            } else if ("setMask".equals(method.getName()) && argument instanceof Mask) {
                mask = (M) argument;
                maskString = null;
                return null;
            } else {
                throw new RuntimeException("Unrecognized method: " + method);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("asAsync".equals(method.getName()) && method.getDeclaringClass() == Service.class) {
                return asAsync();
            } else if (method.getDeclaringClass() == Maskable.class) {
                return maskInvocation(method, args == null || args.length == 0 ? null : args[0]);
            } else if (Service.class.isAssignableFrom(method.getDeclaringClass())) {
                ApiMethod methodInfo = method.getAnnotation(ApiMethod.class);
                // Must have ID if instance is required
                if (methodInfo.instanceRequired() && id == null) {
                    throw new IllegalStateException("ID is required to invoke " + method);
                }
                String methodName = methodInfo.value().isEmpty() ? method.getName() : methodInfo.value();
                String httpMethod = getHttpMethodFromMethodName(methodName);
                Long methodId = methodInfo.instanceRequired() ? this.id : null;
                String url = getFullUrl(serviceClass.getAnnotation(ApiService.class).value(),
                        methodName, methodId, mask == null ? maskString : mask.getMask());
                HttpClient client = getHttpClientFactory().getHttpClient(credentials,
                        httpMethod, url, HEADERS);
                // Log if enabled
                if (loggingEnabled) {
                    logRequest(httpMethod, url, args);
                }
                // If there are parameters write em
                if (args != null && args.length > 0) {
                    writeParameterHttpBody(args, client.getBodyStream());
                }
                // Invoke with response
                HttpResponse response = client.invokeSync();
                // Log...
                InputStream stream = response.getInputStream();
                if (loggingEnabled) {
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
                // If it's not a 200, we have a problem
                if (response.getStatusCode() != 200) {
                    // Extract error and throw
                    Map<String, String> map = getJsonMarshallerFactory().getJsonMarshaller().
                            fromJson(Map.class, stream);
                    throw ApiException.fromError(map.get("error"), map.get("code"), response.getStatusCode());
                }
                // Just return the serialized response
                return getJsonMarshallerFactory().getJsonMarshaller().fromJson(method.getReturnType(), stream);
            } else if (ServiceAsync.class.isAssignableFrom(method.getDeclaringClass())) {
                throw new UnsupportedOperationException();
            } else {
                // Should not be possible
                throw new RuntimeException("Unrecognized method: " + method);
            }
        }
    }
}
