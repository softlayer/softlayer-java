package com.softlayer.api.gen;

import java.util.List;
import java.util.Map;

public class TypeClass {
    
    public final Meta.Type meta;
    public final Map<String, String> imports;
    public final String packageName;
    public final String className;
    public final String baseJavaType;
    public final Meta.Type baseMeta;
    public final String baseServiceJavaType;
    public final Meta.Type baseServiceMeta;
    public final List<Property> properties;
    public final List<Method> methods;
    
    public TypeClass(Meta.Type meta, Map<String, String> imports, String packageName, String className,
            String baseJavaType, Meta.Type baseMeta, String baseServiceJavaType, Meta.Type baseServiceMeta,
            List<Property> properties, List<Method> methods) {
        this.meta = meta;
        this.imports = imports;
        this.packageName = packageName;
        this.className = className;
        this.baseJavaType = baseJavaType;
        this.baseMeta = baseMeta;
        this.properties = properties;
        this.methods = methods;
        this.baseServiceJavaType = baseServiceJavaType;
        this.baseServiceMeta = baseServiceMeta;
    }
    
    public String getFullClassName() {
        return packageName + '.' + className;
    }

    public static class Property {
        public final Meta.Property meta;
        public final String name;
        public final String javaType;
        public final String nonArrayJavaType;
        
        public Property(Meta.Property meta, String name, String javaType, String nonArrayJavaType) {
            this.meta = meta;
            this.name = name;
            this.javaType = javaType;
            this.nonArrayJavaType = nonArrayJavaType;
        }
    }
    
    public static class Method {
        public final Meta.Method meta;
        public final String name;
        public final String javaType;
        public final List<Parameter> parameters;
        
        public Method(Meta.Method meta, String name, String javaType, List<Parameter> parameters) {
            this.meta = meta;
            this.name = name;
            this.javaType = javaType;
            this.parameters = parameters;
        }
    }
    
    public static class Parameter {
        public final Meta.Parameter meta;
        public final String javaType;
        
        public Parameter(Meta.Parameter meta, String javaType) {
            this.meta = meta;
            this.javaType = javaType;
        }
    }
}
