package com.softlayer.api.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.softlayer.api.annotation.ApiProperty;
import com.softlayer.api.annotation.ApiType;
import com.softlayer.api.annotation.ApiTypes;
import com.softlayer.api.service.Entity;

class GsonJsonMarshallerFactory extends JsonMarshallerFactory implements JsonMarshaller {

    protected final static Gson gson;
    private final static Map<String, Class<? extends Entity>> typeClasses;
    
    static {
        gson = new GsonBuilder().
            disableHtmlEscaping().
            disableInnerClassSerialization().
            registerTypeAdapterFactory(new EntityTypeAdapterFactory()).
            registerTypeAdapter(GregorianCalendar.class, new GregorianCalendarTypeAdapter()).
            serializeNulls().
            create();
        
        ApiTypes types = Entity.class.getPackage().getAnnotation(ApiTypes.class);
        Map<String, Class<? extends Entity>> classes = new HashMap<String, Class<? extends Entity>>(
                types.value().length);
        for (Class<? extends Entity> clazz : types.value()) {
            classes.put(clazz.getAnnotation(ApiType.class).value(), clazz);
        }
        typeClasses = Collections.unmodifiableMap(classes);
    }
    
    @Override
    public JsonMarshaller getJsonMarshaller() {
        return this;
    }

    @Override
    public void toJson(Object object, OutputStream out) {
        gson.toJson(object, object.getClass(), new OutputStreamWriter(out));
    }

    @Override
    public <T> T fromJson(Type type, InputStream in) {
        return gson.fromJson(new InputStreamReader(in), type);
    }
    
    static class EntityTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<?> typeClass = type.getRawType();
            if (!Entity.class.isAssignableFrom(typeClass)) {
                return null;
            }
            // Obtain all ApiProperty fields and make them accessible...
            Map<String, EntityJsonField> fields = new HashMap<String, EntityJsonField>();
            loadFields(typeClass, fields);
            return (TypeAdapter<T>) new EntityTypeAdapter((Class<? extends Entity>) typeClass, fields);
        }
        
        protected void loadFields(Class<?> clazz, Map<String, EntityJsonField> fields) {
            for (Field field : clazz.getDeclaredFields()) {
                ApiProperty property = field.getAnnotation(ApiProperty.class);
                if (property != null) {
                    String name = property.value().isEmpty() ? field.getName() : property.value();
                    if (!fields.containsKey(name)) {
                        field.setAccessible(true);
                        Field specifiedField = null;
                        if (property.canBeNullOrNotSet()) {
                            try {
                                specifiedField = clazz.getDeclaredField(field.getName() + "Specified");
                            } catch (NoSuchFieldException e) {
                                throw new RuntimeException(
                                    "Cannot find specified field for " + name + " on " + clazz, e);
                            }
                            specifiedField.setAccessible(true);
                        }
                        fields.put(name, new EntityJsonField(field, specifiedField));
                    }
                }
            }
            
            if (clazz.getSuperclass() != Object.class) {
                loadFields(clazz.getSuperclass(), fields);
            }
        }
    }
    
    static class EntityJsonField {
        public final Field field;
        public final Field specifiedField;
        
        public EntityJsonField(Field field, Field specifiedField) {
            this.field = field;
            this.specifiedField = specifiedField;
        }
    }
    
    static class EntityTypeAdapter extends TypeAdapter<Entity> {
        
        final Class<? extends Entity> typeClass;
        final String typeName;
        final Map<String, EntityJsonField> fields;
        
        public EntityTypeAdapter(Class<? extends Entity> typeClass, Map<String, EntityJsonField> fields) {
            this.typeClass = typeClass;
            this.typeName = typeClass.getAnnotation(ApiType.class).value();
            this.fields = fields;
        }

        @Override
        public void write(JsonWriter out, Entity value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            // Every type will include the "complexType" field
            out.name("complexType").value(typeName);
            for (Map.Entry<String, EntityJsonField> fieldEntry : fields.entrySet()) {
                EntityJsonField field = fieldEntry.getValue();
                try {
                    Object fieldValue = field.field.get(value);
                    if (fieldValue != null ||
                            (field.specifiedField != null && field.specifiedField.getBoolean(value))) {
                        out.name(fieldEntry.getKey());
                        gson.toJson(fieldValue, field.field.getGenericType(), out);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            out.endObject();
        }

        @Override
        public Entity read(JsonReader in) throws IOException {
            // We are allowed to assume that the first property is apiType always. This allows us to maintain
            //  a streaming reader
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            in.beginObject();
            if (!"apiType".equals(in.nextName())) {
                throw new RuntimeException("Expected 'apiType' as first property");
            }
            String apiTypeName = in.nextString();
            // If the API type is unrecognized by us (i.e. it's a new type), we just use the type
            //  we're an adapter for.
            Class<? extends Entity> clazz = typeClasses.get(apiTypeName);
            Entity result;
            if (clazz == null) {
                result = readForThisType(in);
            } else if (!typeClass.isAssignableFrom(clazz)) {
                throw new RuntimeException("Expecting " + typeClass + " to be super type of " + clazz);
            } else {
                result = ((EntityTypeAdapter) gson.getAdapter(clazz)).readForThisType(in);
            }
            in.endObject();
            return result;
        }
        
        private Entity readForThisType(JsonReader in) throws IOException {
            // Begin/end object (and the first "apiType" property) are done outside of here
            Entity entity;
            try {
                entity = typeClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            Map<String, Object> unknownProperties = new HashMap<String, Object>();
            while (in.hasNext()) {
                String propertyName = in.nextName();
                EntityJsonField field = fields.get(propertyName);
                // No field means we just add the object to the unknown set
                if (field == null) {
                    unknownProperties.put(propertyName, gson.fromJson(in, Object.class));
                } else {
                    try {
                        field.field.set(entity, gson.fromJson(in, field.field.getGenericType()));
                        if (field.specifiedField != null) {
                            field.specifiedField.setBoolean(entity, true);
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (!unknownProperties.isEmpty()) {
                entity.setUnknownProperties(unknownProperties);
            }
            return entity;
        }
    }
    
    static class GregorianCalendarTypeAdapter extends TypeAdapter<GregorianCalendar> {
        
        // Although this is ISO-8601, Java 6 does not allow a colon in the timestamp. All API
        //  dates look like this: 1984-02-25T20:15:25-06:00. Since we can guarantee that, we
        //  can just remove/add the colon as necessary. This is a better solution than using
        //  JAXB libraries.
        // Ref: http://stackoverflow.com/questions/2201925/converting-iso-8601-compliant-string-to-java-util-date
        
        final ThreadLocal<DateFormat> format = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            }
        };

        @Override
        public void write(JsonWriter out, GregorianCalendar value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                String date = format.get().format(value.getTime());
                // Add the colon
                out.value(date.substring(0, date.length() - 2) + ':' + date.substring(date.length() - 2));
            }
        }

        @Override
        public GregorianCalendar read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String date = in.nextString();
            // Remove the colon
            date = date.substring(0, date.length() - 3) + date.substring(date.length() - 2);
            GregorianCalendar calendar = new GregorianCalendar();
            try {
                calendar.setTime(format.get().parse(date));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            return calendar;
        }
        
    }
}
