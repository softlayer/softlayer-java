package com.softlayer.api.json;

import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class JsonMarshallerFactory {

    public static JsonMarshallerFactory getDefault() {
        Iterator<JsonMarshallerFactory> iterator = ServiceLoader.load(JsonMarshallerFactory.class).iterator();
        if (!iterator.hasNext()) {
            // Default to Jackson (which may not be present, but we just let NoClassDefFoundError throw)
            return new JacksonJsonMarshallerFactory();
        }
        JsonMarshallerFactory factory = iterator.next();
        if (iterator.hasNext()) {
            throw new RuntimeException("Ambiguous JSON marshaller factories: " + factory.getClass() +
                    ", " + iterator.next().getClass() + " and possibly more");
        }
        return factory;
    }
    
    public abstract JsonMarshaller getJsonMarshaller();
}
