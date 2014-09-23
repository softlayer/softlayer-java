package com.softlayer.api.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.softlayer.api.temp.Entity;

class JacksonJsonMarshallerFactory extends JsonMarshallerFactory {

    final ObjectMapper mapper;
    
    public JacksonJsonMarshallerFactory() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("SoftLayerEntity");
        module = module.addSerializer(Entity.class, new EntitySerializer());
        mapper.registerModule(module);
    }
    
    @Override
    public JsonMarshaller getJsonMarshaller() {
        return new JacksonJsonMarshaller();
    }
    
    class JacksonJsonMarshaller implements JsonMarshaller {

        @Override
        public void toJson(Object object, OutputStream out) {
            try {
                mapper.writeValue(out, object);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T fromJson(Type type, InputStream in) {
            try {
                return mapper.readValue(in, mapper.getTypeFactory().constructType(type));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    static class EntitySerializer extends StdSerializer<Entity> {

        protected EntitySerializer() {
            super(Entity.class);
        }

        @Override
        public void serialize(Entity value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonGenerationException {
            throw new UnsupportedOperationException();
        }
        
    }
}
