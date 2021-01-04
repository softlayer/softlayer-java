package com.softlayer.api.http;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpBearerCredentialsTest {

    public final String bearerToken = "qqqqwwwweeerrttyyuuiiooppasddfgfgjghjkjklZXxcvcvbvbnnbm";
    @Test
    public void testConstructor() {
        HttpBearerCredentials authCredentials = new HttpBearerCredentials(bearerToken);
        assertEquals(bearerToken, authCredentials.token);
    }

    @Test
    public void testGetHeader() {
        HttpBearerCredentials authCredentials = new HttpBearerCredentials(bearerToken);
        String header = "Bearer " + bearerToken;
        assertEquals(header, authCredentials.getHeader());
    }
}
