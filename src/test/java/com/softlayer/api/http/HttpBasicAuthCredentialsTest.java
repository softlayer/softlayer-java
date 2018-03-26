package com.softlayer.api.http;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpBasicAuthCredentialsTest {

    @Test
    public void testConstructor()
    {
        HttpBasicAuthCredentials authCredentials = new HttpBasicAuthCredentials("username", "apiKey");
        assertEquals("username", authCredentials.username);
        assertEquals("apiKey", authCredentials.apiKey);
    }

    @Test
    public void testGetHeader()
    {
        HttpBasicAuthCredentials authCredentials = new HttpBasicAuthCredentials("username", "apiKey");
        assertEquals("Basic dXNlcm5hbWU6YXBpS2V5", authCredentials.getHeader());
    }
}
