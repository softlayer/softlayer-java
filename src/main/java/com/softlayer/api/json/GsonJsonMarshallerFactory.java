package com.softlayer.api.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    final static Map<String, Class<? extends Entity>> typeClasses;
    
    static {
        gson = new GsonBuilder().
            disableHtmlEscaping().
            disableInnerClassSerialization().
            // Two types need special attention: Entity (all non-scalars basically) and GregorianCalendar
            registerTypeAdapterFactory(new EntityTypeAdapterFactory()).
            registerTypeAdapter(GregorianCalendar.class, new GregorianCalendarTypeAdapter()).
            registerTypeAdapter(BigInteger.class, new BigIntegerTypeAdapter()).
            serializeNulls().
            create();
        
        ApiTypes types = Entity.class.getPackage().getAnnotation(ApiTypes.class);
        typeClasses = new HashMap<String, Class<? extends Entity>>(types.value().length);
        for (Class<? extends Entity> clazz : types.value()) {
            typeClasses.put(clazz.getAnnotation(ApiType.class).value(), clazz);
        }
    }
    
    @Override
    public JsonMarshaller getJsonMarshaller() {
        // We can just reuse the common marshaller here to remain performant
        return this;
    }

    @Override
    public void toJson(Object object, OutputStream out) {
        Writer writer = new OutputStreamWriter(out);
        gson.toJson(object, object.getClass(), writer);
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            //  a streaming reader and is very important.
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            in.beginObject();
            if (!"complexType".equals(in.nextName())) {
                throw new RuntimeException("Expected 'complexType' as first property");
            }
            String apiTypeName = in.nextString();
            // If the API type is unrecognized by us (i.e. it's a new type), we just use the type
            //  we're an adapter for. So if we have SoftLayer_Something and a newer release of the
            //  API has a type extending it but we don't have a generated class for it, it will get
            //  properly serialized to a SoftLayer_Something.
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
        
        final ThreadLocal<DateFormat> secondFormat = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            }
        };
        
        // Some times come back with fractions of a second all the way down to 6 digits.
        //  Luckily we can just use the presence of a decimal point as a discriminator between
        //  this format and the one above.
        final ThreadLocal<DateFormat> subSecondFormat = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            }
        };

        @Override
        public void write(JsonWriter out, GregorianCalendar value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                // Always use second-level format here
                String date = secondFormat.get().format(value.getTime());
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
            // Use decimal precense to determine format and trim to ms precision
            DateFormat format;
            int decimalIndex = date.indexOf('.');
            if (decimalIndex != -1) {
                date = trimToMillisecondPrecision(date, decimalIndex);
                format = subSecondFormat.get();
            } else {
                format = secondFormat.get();
            }
            
            GregorianCalendar calendar = new GregorianCalendar();
            try {
                calendar.setTime(format.parse(date));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            return calendar;
        }
        
        private String trimToMillisecondPrecision(String date, int decimalIndex) {
            // If there is a decimal we have to only keep the first three numeric characters
            //  after it (and pad with 0's if fewer than three)
            StringBuilder newDate = new StringBuilder(date);
            int offset = 1;
            do {
                if (!Character.isDigit(newDate.charAt(decimalIndex + offset))) {
                    switch (offset) {
                        case 1:
                            newDate.insert(decimalIndex + offset, "000");
                            break;
                        case 2:
                            newDate.insert(decimalIndex + offset, "00");
                            break;
                        case 3:
                            newDate.insert(decimalIndex + offset, "0");
                            break;
                    }
                    break;
                } else if (offset > 3) {
                    newDate.deleteCharAt(decimalIndex + offset);
                } else {
                    offset++;
                }
            } while (true);
            return newDate.toString();
        }
    }
    
    static class BigIntegerTypeAdapter extends TypeAdapter<BigInteger> {

        @Override
        public void write(JsonWriter out, BigInteger value) throws IOException {
            // Just write out the toString
            out.value(value);
        }

        @Override
        public BigInteger read(JsonReader in) throws IOException {
            // Do the BigDecimal parsing and just convert. The regular Gson BigInteger parser doesn't support
            //  exponents like we need to. Basically, the BigDecimal string constructor is better than the
            //  BigInteger one.
            BigDecimal value = gson.fromJson(in, BigDecimal.class);
            return value == null ? null : value.toBigInteger();
        }
    }
}
