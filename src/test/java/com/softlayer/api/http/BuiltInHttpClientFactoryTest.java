package com.softlayer.api.http;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import com.softlayer.api.ResponseHandler;
import com.softlayer.api.http.BuiltInHttpClientFactory.BuiltInHttpClient;

public class BuiltInHttpClientFactoryTest {

    @Test
    public void testGetThreadPoolDefaultsToDaemonThreads() throws Exception {
        boolean daemon = new BuiltInHttpClientFactory().getThreadPool().submit(
            () -> Thread.currentThread().isDaemon()
        ).get();
        assertTrue(daemon);
    }

    @Test
    public void testSetThreadPoolShutsDownNonUserDefined() {
        BuiltInHttpClientFactory factory = new BuiltInHttpClientFactory();
        ExecutorService threadPool = mock(ExecutorService.class);
        factory.threadPool = threadPool;
        factory.threadPoolUserDefined = false;
        factory.setThreadPool(null);
        verify(threadPool).shutdownNow();
    }

    @Test
    public void testSetThreadPoolDoesNotShutDownUserDefined() {
        BuiltInHttpClientFactory factory = new BuiltInHttpClientFactory();
        ExecutorService threadPool = mock(ExecutorService.class);
        factory.threadPool = threadPool;
        factory.threadPoolUserDefined = true;
        factory.setThreadPool(null);
        verify(threadPool, never()).shutdownNow();
    }
    
    @Test
    public void testInvokeSyncSetsUpProperly() throws Exception {
        BuiltInHttpClient client = spy(
            new BuiltInHttpClientFactory().getHttpClient(
                new HttpBasicAuthCredentials("some user", "some key"),
                "NOTGET",
                "http://example.com",
                Collections.singletonMap("header", Collections.singletonList("some header value"))
            )
        );
        client.connection = mock(HttpURLConnection.class);
        // Skip opening connection
        doNothing().when(client).openConnection();
        // Go
        Callable<?> setupBody = mock(Callable.class);
        HttpResponse response = client.invokeSync(setupBody);
        assertEquals(client, response);
        // Make sure credentials are base64'd properly
        verify(client.connection).addRequestProperty("Authorization", "Basic c29tZSB1c2VyOnNvbWUga2V5");
        // And other headers
        verify(client.connection).addRequestProperty("header", "some header value");
        // Proper request method
        verify(client.connection).setRequestMethod("NOTGET");
        // And that setup body was called
        verify(setupBody).call();
    }
    
    @Test
    public void testInvokeAsyncFutureResult() throws Exception {
        BuiltInHttpClient client = spy(
            new BuiltInHttpClientFactory().getHttpClient(
                new HttpBasicAuthCredentials("some user", "some key"),
                "GET",
                "http://example.com",
                Collections.emptyMap()
            )
        );
        Callable<?> callable = mock(Callable.class);
        doReturn(client).when(client).invokeSync(callable);
        assertEquals(client, client.invokeAsync(callable).get());
        verify(client).invokeSync(callable);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testInvokeAsyncCallbackSuccess() throws Exception {
        BuiltInHttpClient client = spy(
            new BuiltInHttpClientFactory().getHttpClient(
                new HttpBasicAuthCredentials("some user", "some key"),
                "GET",
                "http://example.com",
                Collections.emptyMap()
            )
        );
        Callable<?> callable = mock(Callable.class);
        doReturn(client).when(client).invokeSync(callable);
        ResponseHandler<HttpResponse> handler = mock(ResponseHandler.class);
        client.invokeAsync(callable, handler).get();
        verify(client).invokeSync(callable);
        verify(handler).onSuccess(client);
        verify(handler, never()).onError(any(Exception.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testInvokeAsyncCallbackError() throws Exception {
        BuiltInHttpClient client = spy(
            new BuiltInHttpClientFactory().getHttpClient(
                new HttpBasicAuthCredentials("some user", "some key"),
                "GET",
                "http://example.com",
                Collections.emptyMap()
            )
        );
        Callable<?> callable = mock(Callable.class);
        doThrow(RuntimeException.class).when(client).invokeSync(callable);
        ResponseHandler<HttpResponse> handler = mock(ResponseHandler.class);
        client.invokeAsync(callable, handler).get();
        verify(client).invokeSync(callable);
        verify(handler).onError(any(RuntimeException.class));
        verify(handler, never()).onSuccess(any(HttpResponse.class));
    }

    @Test
    public void testGetInputStreamOnSuccess() throws Exception {
        BuiltInHttpClient client = spy(
            new BuiltInHttpClientFactory().getHttpClient(
                new HttpBasicAuthCredentials("some user", "some key"),
                "GET",
                "http://example.com",
                Collections.emptyMap()
            )
        );
        client.connection = mock(HttpURLConnection.class);
        when(client.connection.getResponseCode()).thenReturn(250);
        client.getInputStream();
        verify(client.connection).getInputStream();
        verify(client.connection, never()).getErrorStream();
    }
    
    @Test
    public void testGetErrorStreamOnFailure() throws Exception {
        BuiltInHttpClient client = spy(
            new BuiltInHttpClientFactory().getHttpClient(
                new HttpBasicAuthCredentials("some user", "some key"),
                "GET",
                "http://example.com",
                Collections.emptyMap()
            )
        );
        client.connection = mock(HttpURLConnection.class);
        when(client.connection.getResponseCode()).thenReturn(450);
        client.getInputStream();
        verify(client.connection).getErrorStream();
        verify(client.connection, never()).getInputStream();
    }
}
