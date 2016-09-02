package com.softlayer.api.gen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javawriter.JavaWriter;

public class ClassWriter extends JavaWriter {

    public static final String SLDN_URL_BASE_PATH = "http://sldn.softlayer.com/reference/";
    
    public static final String TYPE_API_CLIENT = "com.softlayer.api.ApiClient";
    public static final String TYPE_API_METHOD = "com.softlayer.api.annotation.ApiMethod";
    public static final String TYPE_API_PROPERTY = "com.softlayer.api.annotation.ApiProperty";
    public static final String TYPE_API_SERVICE = "com.softlayer.api.annotation.ApiService";
    public static final String TYPE_API_TYPE = "com.softlayer.api.annotation.ApiType";
    public static final String TYPE_API_TYPES = "com.softlayer.api.annotation.ApiTypes";
    public static final String TYPE_CALLABLE = "java.util.concurrent.Callable";
    public static final String TYPE_FUTURE = "java.util.concurrent.Future";
    public static final String TYPE_MASK = "com.softlayer.api.Mask";
    public static final String TYPE_RESPONSE_HANDLER = "com.softlayer.api.ResponseHandler";
    public static final String TYPE_SERVICE = "com.softlayer.api.Service";
    public static final String TYPE_SERVICE_ASYNC = "com.softlayer.api.ServiceAsync";
    public static final String TYPE_TYPE = "com.softlayer.api.Type";
    public static final String TYPE_FILTERABLE_BOOLEAN_PROPERTY = "com.softlayer.api.Property.BooleanProperty";
    public static final String TYPE_FILTERABLE_BYTE_ARRAY_PROPERTY = "com.softlayer.api.Property.ByteArrayProperty";
    public static final String TYPE_FILTERABLE_DATE_TIME_PROPERTY = "com.softlayer.api.Property.DateTimeProperty";
    public static final String TYPE_FILTERABLE_NUMBER_PROPERTY = "com.softlayer.api.Property.NumberProperty";
    public static final String TYPE_FILTERABLE_STRING_PROPERTY = "com.softlayer.api.Property.StringProperty";

    private static final Set<Modifier> PROTECTED = EnumSet.of(Modifier.PROTECTED);
    private static final Set<Modifier> PUBLIC = EnumSet.of(Modifier.PUBLIC);
    private static final Set<Modifier> PUBLIC_STATIC = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
    
    public static void emitPackageInfo(File baseDir, List<TypeClass> classes) throws IOException {
        // Do this manually, the Java writer doesn't help us here
        StringBuilder types = new StringBuilder();
        for (TypeClass type : classes) {
            if (types.length() != 0) {
                types.append(",\n");
            }
            types.append("    ").append(type.getFullClassName()).append(".class");
        }
        Writer writer = new BufferedWriter(
            new FileWriter(new File(baseDir, "com/softlayer/api/service/package-info.java")));
        try {
            writer.append("@ApiTypes({\n").append(types).append("\n})\npackage ").
                append(Generator.BASE_PKG).append(";\nimport ").append(TYPE_API_TYPES).append(";\n");
        } finally {
            try { writer.close(); } catch (Exception e) { }
        }
    }
    
    public static void emitType(File baseDir, TypeClass type, Meta meta) throws IOException {
        File fileDir = new File(baseDir, type.packageName.replace('.', '/'));
        fileDir.mkdirs();
        Writer writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(new File(fileDir, type.className + ".java")), "UTF-8"));
        try {
            new ClassWriter(writer, type, meta).emitType();
        } finally {
            try { writer.close(); } catch (Exception e) { }
        }
    }
    
    public static String getClassName(String typeName) {
        // Just the last name after the underscore
        int lastUnderscore = typeName.lastIndexOf('_');
        if (lastUnderscore == -1) {
            return typeName;
        } else {
            return typeName.substring(lastUnderscore + 1);
        }
    }
    
    public static String getClassName(Meta.Type type) {
        return getClassName(type.name);
    }
    
    public final TypeClass type;
    private final Meta meta;
    
    public ClassWriter(Writer out, TypeClass type, Meta meta) {
        super(out);
        this.type = type;
        this.meta = meta;
        setIndent("    ");
    }

    public ClassWriter emitAnnotationWithAttrs(String annotationType, Object... attributes) throws IOException {
        int i = 0;
        Map<String, Object> attrMap = new HashMap<String, Object>(attributes.length / 2 + 1);
        while (i < attributes.length) {
            String key = attributes[i++].toString();
            attrMap.put(key, attributes[i++]);
        }
        emitAnnotation(annotationType, attrMap);
        return this;
    }
    
    public ClassWriter emitMask() throws IOException {

        String baseMask = type.baseJavaType != null ? type.baseJavaType + ".Mask" : TYPE_MASK;
        beginType("Mask", "class", PUBLIC_STATIC, baseMask).emitEmptyLine();

        for (TypeClass.Property property : type.properties) {
            if (property.nonArrayJavaType.startsWith("com.")) {
                beginMethod(property.nonArrayJavaType + ".Mask", property.name, PUBLIC).
                    emitStatement("return withSubMask(%s, %s.class)", stringLiteral(property.name),
                        compressType(property.nonArrayJavaType + ".Mask")).
                    endMethod().emitEmptyLine();
            } else {
                String method;
                String returnType;

                if ("Boolean".equals(property.nonArrayJavaType)) {
                    method = "withBooleanProperty";
                    returnType = TYPE_FILTERABLE_BOOLEAN_PROPERTY;
                } else if ("byte[]".equals(property.nonArrayJavaType)) {
                    method = "withByteArrayProperty";
                    returnType = TYPE_FILTERABLE_BYTE_ARRAY_PROPERTY;
                } else if ("java.util.GregorianCalendar".equals(property.nonArrayJavaType)) {
                    method = "withDateTimeProperty";
                    returnType = TYPE_FILTERABLE_DATE_TIME_PROPERTY;
                } else if ("java.math.BigDecimal".equals(property.nonArrayJavaType)
                        || "java.math.BigInteger".equals(property.nonArrayJavaType)
                        || "Long".equals(property.nonArrayJavaType)) {
                    method = "withNumberProperty";
                    returnType = TYPE_FILTERABLE_NUMBER_PROPERTY;
                } else if ("String".equals(property.nonArrayJavaType)) {
                    method = "withStringProperty";
                    returnType = TYPE_FILTERABLE_STRING_PROPERTY;
                } else {
                    throw new IllegalArgumentException("Unrecognized primitive type: " + property.nonArrayJavaType);
                }
                beginMethod(returnType, property.name, PUBLIC)
                        .emitStatement("return " + method + "(%s)", stringLiteral(property.name))
                        .endMethod()
                        .emitEmptyLine();
            }
        }

        endType().emitEmptyLine();
        return this;
    }
    
    public ClassWriter emitProperty(TypeClass.Property property) throws IOException {
        if (property.meta.doc != null) {
            emitJavadoc(property.meta.doc.replace("\n", "<br />\n"));
        }
        
        Map<String, Object> params = new HashMap<String, Object>(2);
        if (!property.name.equals(property.meta.name)) {
            params.put("value", stringLiteral(property.meta.name));
        }
        if (property.meta.form == Meta.PropertyForm.LOCAL) {
            params.put("canBeNullOrNotSet", true);
        }
        emitAnnotation("ApiProperty", params);
        emitField(property.javaType, property.name, PROTECTED).emitEmptyLine();
        
        // Getter
        String capitalized = Character.toUpperCase(property.name.charAt(0)) +
            property.name.substring(1);
        beginMethod(property.javaType, "get" + capitalized, PUBLIC);
        // If it's an array, we lazily create it here and do not allow it to be set
        if (property.meta.typeArray) {
            beginControlFlow("if (" + property.name + " == null)").
                emitStatement(property.name + " = new " + compressType(
                    property.javaType.replace("List<", "ArrayList<")) + "()").
                endControlFlow().emitStatement("return " + property.name);
            endMethod().emitEmptyLine();
        } else {
            emitStatement("return " + property.name);
            endMethod().emitEmptyLine();
            
            // Setter (which may have to set specified boolean)
            beginMethod("void", "set" + capitalized, PUBLIC, property.javaType, property.name);
            if (property.meta.form == Meta.PropertyForm.LOCAL) {
                emitStatement(property.name + "Specified = true");
            }
            emitStatement("this." + property.name + " = " + property.name).
                endMethod().emitEmptyLine();
        }
        
        // Make the XSpecified boolean property if needed
        if (property.meta.form == Meta.PropertyForm.LOCAL) {
            emitField("boolean", property.name + "Specified", PROTECTED).emitEmptyLine();
            beginMethod("boolean", "is" + capitalized + "Specified", PUBLIC).
                emitStatement("return " + property.name + "Specified").
                endMethod().emitEmptyLine();
            beginMethod("void", "unset" + capitalized, PUBLIC).
                emitStatement(property.name + " = null").
                emitStatement(property.name + "Specified = false").
                endMethod().emitEmptyLine();
        }
        
        return this;
    }
    
    public ClassWriter emitJavadoc(String javadoc, Object... params) throws IOException {
        //Since the base class formats, we have to double-up our percent signs
        return (ClassWriter) super.emitJavadoc(javadoc.replace("%", "%%"), params);
    }
    
    public ClassWriter emitService() throws IOException {
        String javadoc = type.meta.serviceDoc;
        if (javadoc == null) {
            javadoc = "";
        } else {
            javadoc = javadoc.replace("\n", "<br />\n") + "\n\n";
        }
        javadoc += "@see <a href=\"" + SLDN_URL_BASE_PATH + "services/" +
                type.meta.name + "\">" + type.meta.name + "</a>";
        emitJavadoc(javadoc);
        
        emitAnnotation(TYPE_API_SERVICE, stringLiteral(type.meta.name));
        String base;
        if (type.baseServiceJavaType != null) {
            base = compressType(type.baseServiceJavaType) + ".Service";
        } else {
            base = TYPE_SERVICE;
        }
        beginType("Service", "interface", PUBLIC_STATIC, base).emitEmptyLine();
        
        // Covariant return overrides
        beginMethod("ServiceAsync", "asAsync", PUBLIC).endMethod();
        beginMethod("Mask", "withNewMask", PUBLIC).endMethod();
        beginMethod("Mask", "withMask", PUBLIC).endMethod();
        beginMethod("Mask", "withNewFilter", PUBLIC).endMethod();
        beginMethod("Mask", "withFilter", PUBLIC).endMethod().emitEmptyLine();

        for (TypeClass.Method method : type.methods) {
            emitServiceMethod(method, false);
        }
        // Method for every non-local property too...
        for (TypeClass.Property property : type.properties) {
            // Only relational properties support getters
            if (property.meta.form == Meta.PropertyForm.RELATIONAL) {
                emitServiceMethod(property, false);
            }
        }
        endType().emitEmptyLine();
        
        // And the async one...
        if (type.baseServiceJavaType != null) {
            base = compressType(type.baseServiceJavaType) + ".ServiceAsync";
        } else {
            base = TYPE_SERVICE_ASYNC;
        }
        beginType("ServiceAsync", "interface", PUBLIC_STATIC, base).emitEmptyLine();
        
        // Covariant return overrides
        beginMethod("Mask", "withNewMask", PUBLIC).endMethod();
        beginMethod("Mask", "withMask", PUBLIC).endMethod();
        beginMethod("void", "setMask", PUBLIC, "Mask", "mask").endMethod().emitEmptyLine();
        
        for (TypeClass.Method method : type.methods) {
            emitServiceMethod(method, true);
        }
        // Method for every non-local property too...
        for (TypeClass.Property property : type.properties) {
            // Only relational properties support getters
            if (property.meta.form == Meta.PropertyForm.RELATIONAL) {
                emitServiceMethod(property, true);
            }
        }
        endType().emitEmptyLine();
        
        return this;
    }
    
    public ClassWriter emitServiceMethod(TypeClass.Method method, boolean async) throws IOException {
        if (!async) {
            // Only non-async get service docs and annotation
            String javadoc = method.meta.doc;
            if (javadoc == null) {
                javadoc = "";
            } else {
                javadoc = javadoc.replace("\n", "<br />\n") + "\n\n";
            }
            javadoc += "@see <a href=\"" + SLDN_URL_BASE_PATH + "services/" + type.meta.name +
                "/" + method.meta.name + "\">" + type.meta.name + "::" + method.meta.name + "</a>";
            emitJavadoc(javadoc);
            
            Map<String, Object> params = new HashMap<String, Object>(2);
            if (!method.name.equals(method.meta.name)) {
                params.put("value", stringLiteral(method.meta.name));
            }
            if (!method.meta.isstatic) {
                params.put("instanceRequired", true);
            }
            emitAnnotation(TYPE_API_METHOD, params);
        } else {
            // Otherwise, just a javadoc link
            emitJavadoc("Async version of {@link Service#" + method.name + "}");
        }
        
        String[] parameters = new String[method.parameters.size() * 2];
        for (int i = 0; i < method.parameters.size(); i++) {
            TypeClass.Parameter param = method.parameters.get(i);
            parameters[i * 2] = param.javaType;
            parameters[i * 2 + 1] = param.meta.name;
        }
        
        String returnType = method.javaType;
        if (async) {
            returnType = TYPE_FUTURE + '<' + returnType + '>';
        }
        beginMethod(returnType, method.name, PUBLIC, parameters).endMethod().emitEmptyLine();
        
        // Async has an extra callback method
        if (async) {
            parameters = Arrays.copyOf(parameters, parameters.length + 2);
            parameters[parameters.length - 2] = TYPE_RESPONSE_HANDLER + '<' + method.javaType + '>';
            parameters[parameters.length - 1] = "callback";
            beginMethod(TYPE_FUTURE + "<?>", method.name, PUBLIC, parameters).endMethod().emitEmptyLine();
        }
        
        return this;
    }
    
    public ClassWriter emitServiceMethod(TypeClass.Property property, boolean async) throws IOException {
        String capitalized = Character.toUpperCase(property.name.charAt(0)) +
            property.name.substring(1);
        String name = "get" + capitalized;
        if (type.meta.methods.containsKey(name)) {
            return this;
        }
        
        if (!async) {
            // Only non-async get service docs and annotation
            String javadoc = property.meta.doc;
            if (javadoc == null) {
                javadoc = "";
            } else {
                javadoc = javadoc.replace("\n", "<br />\n") + "\n\n";
            }
            javadoc += "@see <a href=\"" + SLDN_URL_BASE_PATH + "services/" + type.meta.name +
                "/" + name + "\">" + type.meta.name + "::" + name + "</a>";
            emitJavadoc(javadoc);
            
            // Instance is only required if it's not an account property
            if ("SoftLayer_Account".equals(type.meta.name)) {
                emitAnnotation(TYPE_API_METHOD);
            } else {
                emitAnnotationWithAttrs(TYPE_API_METHOD, "instanceRequired", true);
            }
        } else {
            // Otherwise, just a javadoc link
            emitJavadoc("Async version of {@link Service#" + name + "}");
        }
        
        String returnType = property.javaType;
        if (async) {
            returnType = TYPE_FUTURE + '<' + returnType + '>';
        }
        beginMethod(returnType, name, PUBLIC).endMethod().emitEmptyLine();
        
        // Async has an extra callback method
        if (async) {
            emitJavadoc("Async callback version of {@link Service#" + name + "}");
            beginMethod(TYPE_FUTURE + "<?>", name, PUBLIC, TYPE_RESPONSE_HANDLER + '<' + property.javaType + '>',
                "callback").endMethod().emitEmptyLine();
        }
        return this;
    }

    public ClassWriter emitType() throws IOException {
        emitPackage(type.packageName);
        
        emitTypeImports();
        
        // Javadoc
        String javadoc = type.meta.typeDoc != null ? type.meta.typeDoc : type.meta.serviceDoc;
        if (javadoc == null) {
            javadoc = "";
        } else {
            javadoc = javadoc.replace("\n", "<br />\n") + "\n\n";
        }
        javadoc += "@see <a href=\"" + SLDN_URL_BASE_PATH + "datatypes/" +
                type.meta.name + "\">" + type.meta.name + "</a>";
        emitJavadoc(javadoc);
        
        // Each type has a type attribute
        emitAnnotation("ApiType", stringLiteral(type.meta.name));
        
        String baseType = type.baseJavaType == null ? TYPE_TYPE : type.baseJavaType;
        beginType(type.className, "class", PUBLIC, baseType).emitEmptyLine();
        
        // Write all the API properties
        for (TypeClass.Property property : type.properties) {
            emitProperty(property);
        }
        
        // Now the service
        if (!type.meta.noservice) {

            // Check if the type or any of its' parent types have id or globalIdentifier properties
            Boolean containsId = false;
            Boolean containsGlobalIdentifier = false;
            Meta.Type searchType = type.meta;
            while (searchType != null) {
                if (searchType.properties.containsKey("id")) {
                    containsId = true;
                    if (searchType.properties.containsKey("globalIdentifier")) {
                        containsGlobalIdentifier = true;
                    }
                    break;
                }
                searchType = meta.types.get(searchType.base);
            }

            if (containsId) {
                if (containsGlobalIdentifier) {
                    beginMethod("Service", "asService", PUBLIC, TYPE_API_CLIENT, "client").
                        beginControlFlow("if (id != null)").
                            emitStatement("return service(client, id)").
                        nextControlFlow("else").
                            emitStatement("return service(client, globalIdentifier)").
                        endControlFlow().endMethod().emitEmptyLine();
                } else {
                    beginMethod("Service", "asService", PUBLIC, TYPE_API_CLIENT, "client").
                        emitStatement("return service(client, id)").endMethod().emitEmptyLine();
                }
            }
            
            beginMethod("Service", "service", PUBLIC_STATIC, TYPE_API_CLIENT, "client").
                emitStatement("return client.createService(Service.class, null)").
                endMethod().emitEmptyLine();

            if (containsId) {
                beginMethod("Service", "service", PUBLIC_STATIC, TYPE_API_CLIENT, "client", "Long", "id").
                    emitStatement("return client.createService(Service.class, id == null ? null : id.toString())").
                    endMethod().emitEmptyLine();
                if (containsGlobalIdentifier) {
                    beginMethod("Service", "service", PUBLIC_STATIC, TYPE_API_CLIENT,
                            "client", "String", "globalIdentifier").
                        emitStatement("return client.createService(Service.class, globalIdentifier)").
                        endMethod().emitEmptyLine();
                }
            }
            
            emitService();
        }

        emitMask().endType();
        return this;
    }
    
    public ClassWriter emitTypeImports() throws IOException {
        Map<String, String> imports = new HashMap<String, String>(type.imports);
        
        imports.remove("Mask");
        imports.remove(type.className);
        imports.put("ApiType", TYPE_API_TYPE);
        
        // If we have properties or methods...
        if (!type.properties.isEmpty()) {
            imports.put("ApiProperty", TYPE_API_PROPERTY);
        }
        if (!type.methods.isEmpty()) {
            imports.put("ApiMethod", TYPE_API_METHOD);
            imports.put("Future", TYPE_FUTURE);
            imports.put("ResponseHandler", TYPE_RESPONSE_HANDLER);
        }
        
        // Remove Service if we have one
        if (!type.meta.noservice) {
            imports.remove("Service");
            imports.remove("ServiceAsync");
            imports.put("ApiClient", TYPE_API_CLIENT);
        }
        
        emitImports(imports.values()).emitEmptyLine();
        return this;
    }
}
