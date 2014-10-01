package com.softlayer.api.json;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

import com.google.gson.reflect.TypeToken;
import com.softlayer.api.annotation.ApiProperty;
import com.softlayer.api.annotation.ApiType;
import com.softlayer.api.service.Entity;

public class GsonJsonMarshallerFactoryTest {
    
    static {
        GsonJsonMarshallerFactory.typeClasses.put("SoftLayer_TestEntity", TestEntity.class);
    }

    @ApiType("SoftLayer_TestEntity")
    public static class TestEntity extends Entity {
        
        @ApiProperty("bar")
        protected String foo;
        
        @ApiProperty(canBeNullOrNotSet = true)
        protected String baz;
        protected boolean bazSpecified;
        
        @ApiProperty
        protected GregorianCalendar date;
        
        protected String notApiProperty;
        
        @ApiProperty
        protected TestEntity child;
        
        @ApiProperty
        protected List<TestEntity> moreChildren;
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
            + "\"apiType\": \"SoftLayer_TestEntity\","
            + "\"bar\": \"some string\","
            + "\"foo\": \"another string\","
            + "\"baz\": null,"
            + "\"date\": \"1984-02-25T20:15:25-06:00\","
            + "\"notApiProperty\": \"bad value\","
            + "\"child\": {"
            + "    \"apiType\": \"SoftLayer_TestEntity\","
            + "    \"bar\": \"child string\""
            + "},"
            + "\"moreChildren\": ["
            + "    { \"apiType\": \"SoftLayer_TestEntity\", \"bar\": \"child 1\" },"
            + "    { \"apiType\": \"SoftLayer_TestEntity\", \"bar\": \"child 2\" }"
            + "]"
            + "}");
        assertEquals(TestEntity.class, entity.getClass());
        TestEntity obj = (TestEntity) entity;
        assertEquals("some string", obj.foo);
        assertEquals(2, obj.getUnknownProperties().size());
        assertEquals("another string", obj.getUnknownProperties().get("foo"));
        assertNull(obj.baz);
        assertTrue(obj.bazSpecified);
        GregorianCalendar expectedDate = new GregorianCalendar(1984, Calendar.FEBRUARY, 25, 20, 15, 25);
        expectedDate.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        assertEquals(expectedDate.getTimeInMillis(), obj.date.getTimeInMillis());
        assertNull(obj.notApiProperty);
        assertEquals("bad value", obj.getUnknownProperties().get("notApiProperty"));
        assertEquals("child string", obj.child.foo);
        assertNull(obj.child.baz);
        assertFalse(obj.child.bazSpecified);
        assertEquals(2, obj.moreChildren.size());
        assertEquals("child 1", obj.moreChildren.get(0).foo);
        assertEquals("child 2", obj.moreChildren.get(1).foo);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testWrite() throws Exception {
        TestEntity obj = new TestEntity();
        obj.foo = "some string";
        obj.baz = null;
        obj.bazSpecified = true;
        obj.date = new GregorianCalendar(1984, Calendar.FEBRUARY, 25, 20, 15, 25);
        obj.date.setTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        obj.notApiProperty = "bad value";
        obj.child = new TestEntity();
        obj.child.foo = "child string";
        obj.moreChildren = Arrays.asList(new TestEntity(), new TestEntity());
        obj.moreChildren.get(0).foo = "child 1";
        obj.moreChildren.get(1).foo = "child 2";
        
        Map<String, Object> actual = fromJson(new TypeToken<Map<String, Object>>() { }.getType(), toJson(obj));
        Map<String, Object> expected = new HashMap<String, Object>(6);
        expected.put("complexType", "SoftLayer_TestEntity");
        expected.put("bar", "some string");
        expected.put("baz", null);
        expected.put("date", "1984-02-25T20:15:25-06:00");
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
}
