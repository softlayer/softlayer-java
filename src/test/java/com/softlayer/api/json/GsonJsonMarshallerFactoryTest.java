package com.softlayer.api.json;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

import com.google.gson.reflect.TypeToken;
import com.softlayer.api.service.Entity;
import com.softlayer.api.service.TestEntity;

public class GsonJsonMarshallerFactoryTest {
    
    static {
        addTestEntityToGson();
    }
    
    public static void addTestEntityToGson() {
        GsonJsonMarshallerFactory.typeClasses.put("SoftLayer_TestEntity", TestEntity.class);
    }

    private <T> T fromJson(Type type, String json) throws Exception {
        return new GsonJsonMarshallerFactory().getJsonMarshaller().
                fromJson(type, new ByteArrayInputStream(json.getBytes("UTF-8")));
    }
    
    private <T> T fromJson(Class<T> clazz, String json) throws Exception {
        return fromJson((Type) clazz, json);
    }
    
    private String toJson(Object obj) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new GsonJsonMarshallerFactory().getJsonMarshaller().toJson(obj, stream);
        return stream.toString("UTF-8");
    }
    
    @Test
    public void testRead() throws Exception {
        Entity entity = fromJson(Entity.class,
            "{"
            + "\"complexType\": \"SoftLayer_TestEntity\","
            + "\"bar\": \"some string\","
            + "\"foo\": \"another string\","
            + "\"baz\": null,"
            + "\"date\": \"1984-02-25T20:15:25-06:00\","
            + "\"notApiProperty\": \"bad value\","
            + "\"child\": {"
            + "    \"complexType\": \"SoftLayer_TestEntity\","
            + "    \"bar\": \"child string\""
            + "},"
            + "\"moreChildren\": ["
            + "    { \"complexType\": \"SoftLayer_TestEntity\", \"bar\": \"child 1\" },"
            + "    { \"complexType\": \"SoftLayer_TestEntity\", \"bar\": \"child 2\" }"
            + "]"
            + "}");
        assertEquals(TestEntity.class, entity.getClass());
        TestEntity obj = (TestEntity) entity;
        assertEquals("some string", obj.getFoo());
        assertEquals(2, obj.getUnknownProperties().size());
        assertEquals("another string", obj.getUnknownProperties().get("foo"));
        assertNull(obj.getBaz());
        assertTrue(obj.isBazSpecified());
        GregorianCalendar expectedDate = new GregorianCalendar(1984, Calendar.FEBRUARY, 25, 20, 15, 25);
        expectedDate.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        assertEquals(expectedDate.getTimeInMillis(), obj.getDate().getTimeInMillis());
        assertNull(obj.getNotApiProperty());
        assertEquals("bad value", obj.getUnknownProperties().get("notApiProperty"));
        assertEquals("child string", obj.getChild().getFoo());
        assertNull(obj.getChild().getBaz());
        assertFalse(obj.getChild().isBazSpecified());
        assertEquals(2, obj.getMoreChildren().size());
        assertEquals("child 1", obj.getMoreChildren().get(0).getFoo());
        assertEquals("child 2", obj.getMoreChildren().get(1).getFoo());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testWrite() throws Exception {
        TestEntity obj = new TestEntity();
        obj.setFoo("some string");
        obj.setBaz(null);
        obj.setDate(new GregorianCalendar(1984, Calendar.FEBRUARY, 25, 20, 15, 25));
        obj.setNotApiProperty("bad value");
        obj.setChild(new TestEntity());
        obj.getChild().setFoo("child string");
        obj.getMoreChildren().add(new TestEntity());
        obj.getMoreChildren().add(new TestEntity());
        obj.getMoreChildren().get(0).setFoo("child 1");
        obj.getMoreChildren().get(1).setFoo("child 2");
        
        Map<String, Object> actual = fromJson(new TypeToken<Map<String, Object>>() { }.getType(), toJson(obj));
        Map<String, Object> expected = new HashMap<String, Object>(6);
        expected.put("complexType", "SoftLayer_TestEntity");
        expected.put("bar", "some string");
        expected.put("baz", null);
        int offsetMinutes = TimeZone.getDefault().getOffset(obj.getDate().getTimeInMillis()) / 60000;
        String expectedTimeZone =
            (offsetMinutes < 0 ? '-' : '+') +
            String.format("%1$02d:%2$02d", Math.abs(offsetMinutes / 60), Math.abs(offsetMinutes % 60));
        expected.put("date", "1984-02-25T20:15:25" + expectedTimeZone);
        Map<String, Object> childMap = new HashMap<String, Object>();
        childMap.put("complexType", "SoftLayer_TestEntity");
        childMap.put("bar", "child string");
        expected.put("child", childMap);
        Map<String, Object> child1Map = new HashMap<String, Object>();
        child1Map.put("complexType", "SoftLayer_TestEntity");
        child1Map.put("bar", "child 1");
        Map<String, Object> child2Map = new HashMap<String, Object>();
        child2Map.put("complexType", "SoftLayer_TestEntity");
        child2Map.put("bar", "child 2");
        expected.put("moreChildren", Arrays.asList(child1Map, child2Map));
        assertEquals(expected, actual);
    }
    
    @Test
    public void testReadBothDateFormats() throws Exception {
        String regular = "\"1984-02-25T20:15:25-06:00\"";
        Calendar expected = new GregorianCalendar(1984, Calendar.FEBRUARY, 25, 20, 15, 25);
        expected.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        assertEquals(expected.getTimeInMillis(),
            fromJson(GregorianCalendar.class, regular).getTimeInMillis());
        
        String subSecondNoDigits = "\"1984-02-25T20:15:25.-06:00\"";
        assertEquals(expected.getTimeInMillis(),
            fromJson(GregorianCalendar.class, subSecondNoDigits).getTimeInMillis());
        
        String subSecondOneDigit = "\"1984-02-25T20:15:25.1-06:00\"";
        assertEquals(expected.getTimeInMillis() + 100,
            fromJson(GregorianCalendar.class, subSecondOneDigit).getTimeInMillis());
        
        String subSecondTwoDigits = "\"1984-02-25T20:15:25.12-06:00\"";
        assertEquals(expected.getTimeInMillis() + 120,
            fromJson(GregorianCalendar.class, subSecondTwoDigits).getTimeInMillis());
        
        String subSecondThreeDigits = "\"1984-02-25T20:15:25.123-06:00\"";
        assertEquals(expected.getTimeInMillis() + 123,
            fromJson(GregorianCalendar.class, subSecondThreeDigits).getTimeInMillis());
        
        String subSecondFourDigits = "\"1984-02-25T20:15:25.1239-06:00\"";
        assertEquals(expected.getTimeInMillis() + 123,
            fromJson(GregorianCalendar.class, subSecondFourDigits).getTimeInMillis());
        
        String subSecondSixDigits = "\"1984-02-25T20:15:25.123987-06:00\"";
        assertEquals(expected.getTimeInMillis() + 123,
            fromJson(GregorianCalendar.class, subSecondSixDigits).getTimeInMillis());
        
        String subSecondTenDigits = "\"1984-02-25T20:15:25.1239876543-06:00\"";
        assertEquals(expected.getTimeInMillis() + 123,
            fromJson(GregorianCalendar.class, subSecondTenDigits).getTimeInMillis());
    }
    
    @Test
    public void testBigIntegerWithExponents() throws Exception {
        assertEquals(BigInteger.valueOf(123), fromJson(BigInteger.class, "123"));
        assertEquals(new BigInteger("18446744073710000000"), fromJson(BigInteger.class, "1.844674407371e+19"));
    }
    
    @Test
    public void testBase64ByteArrays() throws Exception {
        // Read entire logo into byte array (sigh, have to be Java 6 compatible w/ no extra libs)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = getClass().getResourceAsStream("sl-logo.png");
        try {
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
        } finally {
            try {
                in.close();
            } catch (Exception e) { }
        }
        byte[] bytes = out.toByteArray();
        // We know from testing that the base 64 encoded amount is 2232 chars long
        String json = toJson(Collections.singletonMap("foo", bytes));
        @SuppressWarnings("unchecked")
        Map<String, String> map = fromJson(Map.class, json);
        assertEquals(1, map.size());
        assertEquals(2232, ((String) map.get("foo")).length());
        Map<String, byte[]> byteMap = fromJson(new TypeToken<Map<String, byte[]>>(){}.getType(), json);
        assertEquals(1, map.size());
        assertTrue(Arrays.equals(bytes, byteMap.get("foo")));
    }
}
