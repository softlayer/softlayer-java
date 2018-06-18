package com.softlayer.api;

import com.softlayer.api.annotation.ApiMethod;
import com.softlayer.api.annotation.ApiService;
import com.softlayer.api.http.HttpBasicAuthCredentials;
import com.softlayer.api.http.HttpClient;
import com.softlayer.api.http.HttpResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TestApiClient extends RestApiClient{


    private HttpBasicAuthCredentials credentials;
    public HttpBasicAuthCredentials getCredentials() {
        return credentials;
    }

    public TestApiClient(String baseUrl) {
        super(baseUrl);
    }

    public <S extends Service> S createService(Class<S> serviceClass, String id) {
        return (S) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { serviceClass }, new TestServiceProxy<>(serviceClass, id));
    }

    class TestServiceProxy<S extends Service> extends ServiceProxy {

        final Class<S> serviceClass;
        final String id;
        Mask mask;
        String maskString;
        ResultLimit resultLimit;
        Integer lastResponseTotalItemCount;

        public TestServiceProxy(Class serviceClass, String id) {
            super(serviceClass, id);
            this.serviceClass = serviceClass;
            this.id = id;

        }

        public String invokeService(Method method, final Object[] args) throws Throwable {
            System.out.print("invokeTESTService: " + method + "\n");
            ApiMethod methodInfo = method.getAnnotation(ApiMethod.class);
            // Must have ID if instance is required
            if (methodInfo.instanceRequired() && id == null) {
                throw new IllegalStateException("ID is required to invoke " + method);
            }
            String methodName = methodInfo.value().isEmpty() ? method.getName() : methodInfo.value();
            final String httpMethod = getHttpMethodFromMethodName(methodName);
            String methodId = methodInfo.instanceRequired() ? this.id : null;
            final String url = getFullUrl(serviceClass.getAnnotation(ApiService.class).value(),
                    methodName, methodId, resultLimit, mask == null ? maskString : mask.getMask());
            return url;
        }
    }
}
