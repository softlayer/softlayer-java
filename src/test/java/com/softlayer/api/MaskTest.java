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



public class MaskTest {
    static {
        GsonJsonMarshallerFactoryTest.addTestEntityToGson();
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
}

