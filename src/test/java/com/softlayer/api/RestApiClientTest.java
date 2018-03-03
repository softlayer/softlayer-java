package com.softlayer.api;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.softlayer.api.http.FakeHttpClientFactory;
import com.softlayer.api.http.HttpBasicAuthCredentials;
import com.softlayer.api.json.GsonJsonMarshallerFactoryTest;
import com.softlayer.api.service.TestEntity;

public class RestApiClientTest {
    
    static {
        GsonJsonMarshallerFactoryTest.addTestEntityToGson();
    }
    
    @Test
    public void testConstructorAppendsUrlSlash() {
        assertEquals("http://example.com/", new RestApiClient("http://example.com").getBaseUrl());
    }
    
    @Test
    public void testGetHttpMethodFromMethodName() {
        RestApiClient client = new RestApiClient();
        assertEquals("DELETE", client.getHttpMethodFromMethodName("deleteObject"));
        assertEquals("POST", client.getHttpMethodFromMethodName("createObject"));
        assertEquals("POST", client.getHttpMethodFromMethodName("createObjects"));
        assertEquals("PUT", client.getHttpMethodFromMethodName("editObject"));
        assertEquals("PUT", client.getHttpMethodFromMethodName("editObjects"));
        assertEquals("GET", client.getHttpMethodFromMethodName("blahblahblah"));
    }
    
    @Test
    public void testGetFullUrl() {
        RestApiClient client = new RestApiClient("http://example.com/");
        assertEquals("http://example.com/SomeService/someMethod.json",
            client.getFullUrl("SomeService", "someMethod", null, null, null));
        assertEquals("http://example.com/SomeService/1234/someMethod.json",
            client.getFullUrl("SomeService", "someMethod", "1234", null, null));
        assertEquals("http://example.com/SomeService/1234/someMethod.json?resultLimit=5,6",
            client.getFullUrl("SomeService", "someMethod", "1234", new ResultLimit(5, 6), null));
        assertEquals("http://example.com/SomeService/1234/someMethod.json?resultLimit=5,6&objectMask=someMask%26%26",
            client.getFullUrl("SomeService", "someMethod", "1234", new ResultLimit(5, 6), "someMask&&"));
        assertEquals("http://example.com/SomeService/1234/someMethod.json?objectMask=someMask%26%26",
            client.getFullUrl("SomeService", "someMethod", "1234", null, "someMask&&"));
        assertEquals("http://example.com/SomeService/Something.json",
            client.getFullUrl("SomeService", "getSomething", null, null, null));
        assertEquals("http://example.com/SomeService.json",
            client.getFullUrl("SomeService", "getObject", null, null, null));
        assertEquals("http://example.com/SomeService.json",
            client.getFullUrl("SomeService", "deleteObject", null, null, null));
        assertEquals("http://example.com/SomeService.json",
            client.getFullUrl("SomeService", "createObject", null, null, null));
        assertEquals("http://example.com/SomeService/createObjects.json",
            client.getFullUrl("SomeService", "createObjects", null, null, null));
        assertEquals("http://example.com/SomeService.json",
            client.getFullUrl("SomeService", "editObject", null, null, null));
        assertEquals("http://example.com/SomeService.json",
            client.getFullUrl("SomeService", "editObjects", null, null, null));
    }
    
    private String withOutputCaptured(Callable<?> closure) throws Exception {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
            closure.call();
            return out.toString("UTF-8");
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    public void testLogRequest() throws Exception {
        assertEquals(
            "Running VERB on URL with no body\n",
            withOutputCaptured(() -> {
                new RestApiClient().logRequest("VERB", "URL", new Object[0]);
                return null;
            })
        );
        assertEquals(
            "Running VERB on URL with body: {\"parameters\":[{\"key\":\"value\"}]}\n",
            withOutputCaptured(() -> {
                new RestApiClient().logRequest("VERB", "URL",
                    new Object[]{Collections.singletonMap("key", "value")});
                return null;
            })
        );
    }
    
    @Test
    public void testLogResponse() throws Exception {
        assertEquals(
            "Got 123 on URL with body: some body\n",
            withOutputCaptured(() -> {
                new RestApiClient().logResponse("URL", 123, "some body");
                return null;
            })
        );
    }
    
    @Test(expected = IllegalStateException.class)
    public void testFailedIfCallingNonStaticWithoutId() {
        FakeHttpClientFactory http = new FakeHttpClientFactory(123,
            Collections.emptyMap(), "some response");
        RestApiClient client = new RestApiClient("http://example.com/");
        client.setHttpClientFactory(http);
        TestEntity.service(client).doSomethingNonStatic(new GregorianCalendar());
    }
    
    @Test(expected = IllegalStateException.class)
    public void testFailedIfCallingNonStaticAsyncWithoutId() {
        FakeHttpClientFactory http = new FakeHttpClientFactory(123,
            Collections.emptyMap(), "some response");
        RestApiClient client = new RestApiClient("http://example.com/");
        client.setHttpClientFactory(http);
        TestEntity.service(client).asAsync().doSomethingNonStatic(new GregorianCalendar());
    }
    
    @Test
    public void testSuccess() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/").withCredentials("user", "key");
        client.setHttpClientFactory(http);
        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        assertEquals("some response", TestEntity.service(client).doSomethingStatic(123L, entity));
        assertEquals("user", ((HttpBasicAuthCredentials) http.credentials).username);
        assertEquals("key", ((HttpBasicAuthCredentials) http.credentials).apiKey);
        assertEquals("GET", http.method);
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json", http.fullUrl);
        assertEquals("{\"parameters\":[123,{\"complexType\":\"SoftLayer_TestEntity\",\"bar\":\"blah\"}]}",
            http.outStream.toString("UTF-8"));
        assertEquals(RestApiClient.HEADERS, http.headers);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testBadRequestFailure() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(ApiException.BadRequest.STATUS,
            Collections.emptyMap(),
            "{\"error\": \"some error\", \"code\": \"some code\"}");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);
        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        try {
            TestEntity.service(client).doSomethingStatic(123L, entity);
            fail();
        } catch (ApiException.BadRequest e) {
            assertEquals("some error", e.getMessage());
            assertEquals("some code", e.code);
        }
    }
    
    @Test
    public void testAsyncFutureSuccess() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");

        assertEquals("some response", TestEntity.service(client).asAsync().doSomethingStatic(123L, entity).get());
        assertEquals("user", ((HttpBasicAuthCredentials) http.credentials).username);
        assertEquals("key", ((HttpBasicAuthCredentials) http.credentials).apiKey);
        assertEquals("GET", http.method);
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json", http.fullUrl);
        assertEquals("{\"parameters\":[123,{\"complexType\":\"SoftLayer_TestEntity\",\"bar\":\"blah\"}]}",
            http.outStream.toString("UTF-8"));
        assertEquals(RestApiClient.HEADERS, http.headers);
        assertTrue(http.invokeAsyncFutureCalled);
    }
    
    @Test
    public void testAsyncFutureFailure() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(ApiException.BadRequest.STATUS,
            Collections.emptyMap(),
            "{\"error\": \"some error\", \"code\": \"some code\"}");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        try {
            TestEntity.service(client).asAsync().doSomethingStatic(123L, entity).get();
            fail();
        } catch (ExecutionException e) {
            assertEquals("some error", ((ApiException.BadRequest) e.getCause()).getMessage());
            assertEquals("some code", ((ApiException.BadRequest) e.getCause()).code);
        }
    }
    
    @Test
    public void testAsyncCallbackSuccess() throws Exception {
        final FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        final AtomicBoolean successCalled = new AtomicBoolean();
        TestEntity.service(client).asAsync().doSomethingStatic(123L, entity, new ResponseHandler<String>() {
            @Override
            public void onError(Exception ex) {
                fail();
            }

            @Override
            public void onSuccess(String value) {
                assertEquals("some response", value);
                successCalled.set(true);
            }
        }).get();

        assertEquals("user", ((HttpBasicAuthCredentials) http.credentials).username);
        assertEquals("key", ((HttpBasicAuthCredentials) http.credentials).apiKey);
        assertEquals("GET", http.method);
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json", http.fullUrl);
        assertEquals("{\"parameters\":[123,{\"complexType\":\"SoftLayer_TestEntity\",\"bar\":\"blah\"}]}",
            http.outStream.toString("UTF-8"));
        assertEquals(RestApiClient.HEADERS, http.headers);
        assertTrue(http.invokeAsyncCallbackCalled);
        assertTrue(successCalled.get());
    }
    
    @Test
    public void testAsyncCallbackFailure() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(ApiException.BadRequest.STATUS,
            Collections.emptyMap(),
            "{\"error\": \"some error\", \"code\": \"some code\"}");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        final AtomicBoolean errorCalled = new AtomicBoolean();
        TestEntity.service(client).asAsync().doSomethingStatic(123L, entity, new ResponseHandler<String>() {
            @Override
            public void onError(Exception ex) {
                errorCalled.set(true);
                assertEquals("some error", ((ApiException.BadRequest) ex).getMessage());
                assertEquals("some code", ((ApiException.BadRequest) ex).code);
            }

            @Override
            public void onSuccess(String value) {
                fail();
            }
        }).get();
        assertTrue(errorCalled.get());
    }
    
    @Test
    public void testCallWithLog() throws Exception {
        final FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        String output = withOutputCaptured(() -> {
                RestApiClient client = new RestApiClient("http://example.com/")
                    .withCredentials("user", "key")
                    .withLoggingEnabled();
                client.setHttpClientFactory(http);

                TestEntity entity = new TestEntity();
                entity.setFoo("blah");
                TestEntity.service(client).doSomethingStatic(123L, entity);
                return null;
        });

        assertTrue(http.invokeSyncCalled);
        assertEquals(
            "Running GET on http://example.com/SoftLayer_TestEntity/doSomethingStatic.json with body: "
                + "{\"parameters\":[123,{\"complexType\":\"SoftLayer_TestEntity\",\"bar\":\"blah\"}]}\n"
                + "Got 200 on http://example.com/SoftLayer_TestEntity/doSomethingStatic.json with body: "
                + "\"some response\"\n",
            output);
    }
    
    @Test
    public void testDifferentMethodName() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "[]");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        assertEquals(Collections.emptyList(), TestEntity.service(client).fakeName());
        assertEquals("http://example.com/SoftLayer_TestEntity/actualName.json", http.fullUrl);
        assertNull(http.outStream);
        assertEquals(RestApiClient.HEADERS, http.headers);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testWithMask() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);
        service.withMask().foo().child().date();
        service.withMask().child().baz();

        assertEquals("some response", service.doSomethingStatic(123L, entity));
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json"
            + "?objectMask=" + URLEncoder.encode(service.withMask().getMask(), "UTF-8"), http.fullUrl);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testSetObjectMask() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);
        TestEntity.Mask mask = new TestEntity.Mask();
        mask.foo().child().date();
        mask.child().baz();
        service.setMask(mask);

        assertEquals("some response", service.doSomethingStatic(123L, entity));
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json"
            + "?objectMask=" + URLEncoder.encode(mask.getMask(), "UTF-8"), http.fullUrl);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testSetStringMask() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);
        service.setMask("yay-a-mask");

        assertEquals("some response", service.doSomethingStatic(123L, entity));
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json"
            + "?objectMask=yay-a-mask", http.fullUrl);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testWithResultLimit() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);
        service.setResultLimit(new ResultLimit(1, 2));

        assertEquals(1, service.getResultLimit().offset);
        assertEquals(2, service.getResultLimit().limit);
        assertEquals("some response", service.doSomethingStatic(123L, entity));
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json"
            + "?resultLimit=1,2", http.fullUrl);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testWithTotalItemsResponseHeader() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.singletonMap("SoftLayer-Total-Items", Collections.singletonList("234")),
            "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);

        assertEquals("some response", service.doSomethingStatic(123L, entity));
        assertTrue(http.invokeSyncCalled);
        assertEquals(234, service.getLastResponseTotalItemCount().intValue());
    }
    
    @Test
    public void testWithTotalItemsAsyncFutureResponseHeader() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.singletonMap("SoftLayer-Total-Items", Collections.singletonList("234")),
            "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);

        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.ServiceAsync service = TestEntity.service(client).asAsync();

        assertEquals("some response", service.doSomethingStatic(123L, entity).get());
        assertTrue(http.invokeAsyncFutureCalled);
        assertEquals(234, service.getLastResponseTotalItemCount().intValue());
    }
    
    @Test
    public void testWithTotalItemsAsyncCallbackResponseHeader() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.singletonMap("SoftLayer-Total-Items", Collections.singletonList("234")),
            "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/")
            .withCredentials("user", "key");
        client.setHttpClientFactory(http);
        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.ServiceAsync service = TestEntity.service(client).asAsync();
        final AtomicBoolean successCalled = new AtomicBoolean();
        service.doSomethingStatic(123L, entity, new ResponseHandlerWithHeaders<String>() {
            @Override
            public void onError(Exception ex) {
                fail();
            }

            @Override
            public void onSuccess(String value) {
                assertEquals("some response", value);
                assertEquals(234, getLastResponseTotalItemCount().intValue());
                successCalled.set(true);
            }
        }).get();
        assertTrue(http.invokeAsyncCallbackCalled);
        assertTrue(successCalled.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaskMustNotBeNull() {
        RestApiClient client = new RestApiClient("http://example.com/");
        TestEntity.Service service = TestEntity.service(client);
        service.setMask((Mask) null);
    }
    
    @Test
    public void testMaskRemoval() {
        RestApiClient client = new RestApiClient("http://example.com/");
        TestEntity.Service service = TestEntity.service(client);
        service.withMask().baz();
        assertEquals("baz", service.withMask().toString());
        service.clearMask();
        assertEquals("", service.withMask().toString());
    }

    @Test
    public void testNormalObjectMethodsOnService() {
        RestApiClient client = new RestApiClient("http://example.com/");
        TestEntity.Service service = TestEntity.service(client);
        assertEquals("Service: SoftLayer_TestEntity", service.toString());
        assertEquals("Service: SoftLayer_TestEntity with ID 5", TestEntity.service(client, 5L).toString());
        assertTrue(Proxy.isProxyClass(service.getClass()));
        assertEquals(service.hashCode(), service.hashCode());
        assertTrue(service.equals(service));
    }
}
