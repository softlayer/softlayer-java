package com.softlayer.api.gen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Represents the structure of the metadata provided by the API.
 */
public class Meta {

    /**
     * Reads a JSON object from the given metadata URL and generates a new Meta object containing all types.
     *
     * @param url The API metadata URL.
     * @return Meta
     */
    public static Meta fromUrl(URL url) {
        InputStream stream = null;
        try {
            stream = url.openStream();
            Gson gson = new GsonBuilder().registerTypeAdapter(PropertyForm.class, new TypeAdapter<PropertyForm>() {
                    @Override
                    public void write(JsonWriter out, PropertyForm value) throws IOException {
                        out.value(value.name().toLowerCase());
                    }

                    @Override
                    public PropertyForm read(JsonReader in) throws IOException {
                        return PropertyForm.valueOf(in.nextString().toUpperCase());
                    }
                }).create();
            Map<String, Type> types = gson.fromJson(
                    new InputStreamReader(stream),
                    new TypeToken<Map<String, Type>>(){ }.getType()
            );
            return new Meta(types);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Exception e) { }
            }
        }
    }

    public final Map<String, Type> types;

    public Meta(Map<String, Type> types) {
        this.types = types;
    }

    /**
     * Representation of a type in the metadata API.
     */
    public static class Type {
        public static final String BASE_TYPE_NAME = "SoftLayer_Entity";

        public String name;
        public String base;
        public String typeDoc;
        public Map<String, Property> properties = Collections.emptyMap();
        public String serviceDoc;
        public Map<String, Method> methods = Collections.emptyMap();
        public boolean noservice;
    }

    /**
     * Representation of a property in the metadata API.
     */
    public static class Property {
        public String name;
        public String type;
        public boolean typeArray;
        public PropertyForm form;
        public String doc;
    }
    
    public enum PropertyForm {
        LOCAL,
        RELATIONAL,
        COUNT
    }

    /**
     * A representation of a method in the metadata API.
     */
    public static class Method {
        public String name;
        public String type;
        public boolean typeArray;
        public String doc;
        @SerializedName("static")
        public boolean isstatic;
        public boolean noauth;
        public boolean limitable;
        public boolean filterable;
        public boolean maskable;
        public List<Parameter> parameters = Collections.emptyList();
    }

    /**
     * A representation of a parameter in the metadata API.
     */
    public static class Parameter {
        public String name;
        public String type;
        public boolean typeArray;
        public String doc;
        public Object defaultValue;
    }
}
