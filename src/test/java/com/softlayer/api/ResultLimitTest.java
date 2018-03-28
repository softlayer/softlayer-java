package com.softlayer.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class ResultLimitTest {
    @Test
    public void testConstructorWithLimit()
    {
        int limit = 123;
        ResultLimit resultLimit = new ResultLimit(limit);
        assertEquals(0, resultLimit.offset);
        assertEquals(limit, resultLimit.limit);
    }

    @Test
    public void testConstructorWithOffsetAndLimit()
    {
        int limit = 456;
        int offset = 789;
        ResultLimit resultLimit = new ResultLimit(offset, limit);
        assertEquals(offset, resultLimit.offset);
        assertEquals(limit, resultLimit.limit);
    }
}
