package com.softlayer.api.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface JsonMarshaller {
    public void toJson(Object object, OutputStream out);
    public <T> T fromJson(Type type, InputStream in);
}
