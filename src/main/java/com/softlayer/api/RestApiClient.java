package com.softlayer.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.softlayer.api.http.HttpClientFactory;
import com.softlayer.api.json.JsonMarshallerFactory;

public class RestApiClient implements ApiClient {

    public static final String BASE_URL = "https://api.softlayer.com/rest/v3.1/";
    
    private final String baseUrl;
    private HttpClientFactory httpClientFactory;
    private JsonMarshallerFactory jsonMarshallerFactory;
    private String username;
    private String apiKey;
    
    public RestApiClient() {
        this(BASE_URL);
    }
    
    public RestApiClient(String baseUrl) {
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
        setUsername(username);
        setApiKey(apiKey);
        return this;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
                return maskInvocation(method, args.length == 0 ? null : args[0]);
            } else if (Service.class.isAssignableFrom(method.getDeclaringClass())) {
                throw new UnsupportedOperationException();
            } else if (ServiceAsync.class.isAssignableFrom(method.getDeclaringClass())) {
                throw new UnsupportedOperationException();
            } else {
                // Should not be possible
                throw new RuntimeException("Unrecognized method: " + method);
            }
        }
    }
}
