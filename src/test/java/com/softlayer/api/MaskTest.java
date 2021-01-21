package com.softlayer.api;

import static org.junit.Assert.*;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import com.softlayer.api.http.FakeHttpClientFactory;
import com.softlayer.api.json.GsonJsonMarshallerFactoryTest;
import com.softlayer.api.service.TestEntity;

public class MaskTest {
    static {
        GsonJsonMarshallerFactoryTest.addTestEntityToGson();
    }

    @Test
    public void testWithMask() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.<String, List<String>>emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/").withCredentials("user", "key");
        client.setHttpClientFactory(http);
        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);
        service.withMask().foo();
        service.withMask().child().date();
        service.withMask().child().baz();
        assertEquals("some response", service.doSomethingStatic(123L, entity));
        assertEquals("http://example.com/SoftLayer_TestEntity/doSomethingStatic.json"
            + "?objectMask=" + URLEncoder.encode(service.withMask().getMask(), "UTF-8"), http.fullUrl);
        assertTrue(http.invokeSyncCalled);
    }
    
    @Test
    public void testSetObjectMask() throws Exception {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.<String, List<String>>emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/").withCredentials("user", "key");
        client.setHttpClientFactory(http);
        TestEntity entity = new TestEntity();
        entity.setFoo("blah");
        TestEntity.Service service = TestEntity.service(client);
        TestEntity.Mask mask = new TestEntity.Mask();
        mask.foo();
        mask.child().date();
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
            Collections.<String, List<String>>emptyMap(), "\"some response\"");
        RestApiClient client = new RestApiClient("http://example.com/").withCredentials("user", "key");
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

    @Test
    public void testRecursiveMaskAndLocal() {
        RestApiClient client = new RestApiClient("http://example.com/");
        TestEntity.Service service = TestEntity.service(client);
        service.withMask().recursiveProperty().recursiveProperty().baz();
        service.withMask().recursiveProperty().recursiveProperty().foo();
        service.withMask().recursiveProperty().date();
        assertEquals("recursiveProperty[date,recursiveProperty[foo,baz]]",
            service.withMask().toString());
    }

    @Test
    public void testRecursiveMask() {
        RestApiClient client = new RestApiClient("http://example.com/");
        TestEntity.Service service = TestEntity.service(client);
        service.withMask().recursiveProperty().baz();
        service.withMask().recursiveProperty().foo();
        service.withMask().recursiveProperty().date();

        assertEquals("recursiveProperty[date,foo,baz]",
            service.withMask().toString());
    }

    @Test
    public void testMultiLevelMask() {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(),"");
        RestApiClient client = new RestApiClient("http://example.com/");
        client.setHttpClientFactory(http);

        TestEntity.Service service = TestEntity.service(client);
        service.withMask().recursiveProperty().baz();
        service.withMask().recursiveProperty().foo();

        service.withMask().moreChildren().recursiveProperty().baz();
        service.withMask().moreChildren().date();
        String result = service.getRecursiveProperty();

        assertEquals("moreChildren[date,recursiveProperty.baz],recursiveProperty[foo,baz]",
            service.withMask().toString());
    }

    @Test
    public void testNoChangeMaskScope() {
        FakeHttpClientFactory http = new FakeHttpClientFactory(200,
            Collections.emptyMap(),"");
        RestApiClient client = new RestApiClient("http://example.com/");
        client.setHttpClientFactory(http);

        TestEntity.Service service = TestEntity.service(client);
        service.withMask().testThing().id();
        service.withMask().testThing().first();

        TestEntity result = service.getObject();
        assertEquals("testThing[id,first]", service.withMask().toString());
        String expected = "http://example.com/SoftLayer_TestEntity.json?objectMask=mask%5BtestThing%5Bid%2Cfirst%5D%5D";
        assertEquals(expected, http.fullUrl);
    }

    /**
     * This doesn't work due to the issues mentioned in https://github.com/softlayer/softlayer-java/issues/19
     */
    @Test
    @Ignore
    public void testChangeMaskScope() {
        RestApiClient client = new RestApiClient("http://example.com/");

        TestEntity.Service service = TestEntity.service(client);
        service.withMask().recursiveProperty().baz();
        service.withMask().recursiveProperty().foo();

        String result = service.getRecursiveProperty();
        assertEquals("baz,foo", service.withMask().toString());
    }
}

