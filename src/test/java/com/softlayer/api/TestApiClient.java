package com.softlayer.api;

import com.softlayer.api.annotation.ApiMethod;
import com.softlayer.api.annotation.ApiService;
import com.softlayer.api.http.*;
import com.softlayer.api.json.GsonJsonMarshallerFactoryTest;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import org.mockito.Mockito;

public class TestApiClient extends RestApiClient{


    private HttpBasicAuthCredentials credentials;
    public HttpBasicAuthCredentials getCredentials() {
        return credentials;
    }
    public  FakeHttpClientFactory httpClientFactory;
    public TestApiClient(String baseUrl) {
        super(baseUrl);
    }

    public FakeHttpClientFactory getHttpClientFactory() {
        if (httpClientFactory == null) {
            httpClientFactory = new FakeHttpClientFactory(200,
                    Collections.emptyMap(), "");
        }
        return httpClientFactory;
    }
    public void setHttpClientFactory(FakeHttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public <S extends Service> S createService(Class<S> serviceClass, String id) {
        return (S) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { serviceClass }, new TestServiceProxy<>(serviceClass, id));
    }

    class TestServiceProxy<S extends Service> extends ServiceProxy {

        final Class<S> serviceClass;
        final String id;

        public Mask mask;
        public String url;
        String maskString;
        ResultLimit resultLimit;
        Integer lastResponseTotalItemCount;


        public TestServiceProxy(Class<S> serviceClass, String id) {
            super(serviceClass, id);
            Class c = serviceClass;
            try {
                c = Class.forName("com.softlayer.api.service.TestEntity$ServiceTester");
            }catch (ClassNotFoundException x) {
                x.printStackTrace();
            }

            this.serviceClass = c;
            this.id = id;
        }

        @Override
        public Object invokeService(Method method, final Object[] args) throws Throwable {
            this.mask = super.mask;
            this.maskString = super.maskString;

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

            this.url = url;

            Method toCall = serviceClass.getDeclaredMethod(method.getName());
            Object resultInvoke = toCall.invoke(serviceClass.newInstance());
            OutputStream output = new ByteArrayOutputStream();
            getJsonMarshallerFactory().getJsonMarshaller().toJson(
                    resultInvoke, output);
            FakeHttpClientFactory faketory = new FakeHttpClientFactory(200,
                    Collections.emptyMap(), output.toString());
            setHttpClientFactory(faketory);

            final HttpClient client = getHttpClientFactory().getHttpClient(credentials, httpMethod, url, HEADERS);
            HttpResponse response = client.invokeSync(() -> {
                logRequestAndWriteBody(client, httpMethod, url, args);
                return null;
            });
            return logAndHandleResponse(response, url, method.getGenericReturnType());
        }
    }


}
