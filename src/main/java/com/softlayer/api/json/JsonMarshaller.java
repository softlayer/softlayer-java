package com.softlayer.api.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/** 
 * Interface that must be implemented by all JSON marshallers. This instance is not reused unless
 * {@link JsonMarshallerFactory#getJsonMarshaller()} returns the same instance multiple times.
 */
public interface JsonMarshaller {
    
    /** Convert the given object to JSON on the stream. The output stream is closed by this marshaller */
    public void toJson(Object object, OutputStream out);
    
    /** Convert the JSON stream to the given type. The input stream is closed by this marshaller */
    public <T> T fromJson(Type type, InputStream in);
}
