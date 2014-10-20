package com.softlayer.api.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetaConverter {
    
    protected static final Map<String, String> keywordReplacements;
    protected static final Set<String> keywords;
    protected static final Set<String> invalidClassNames;
    
    static {
        keywordReplacements = new HashMap<String, String>(2);
        keywordReplacements.put("package", "pkg");
        keywordReplacements.put("private", "priv");
        keywords = new HashSet<String>(Arrays.asList(new String[] {
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while"
        }));
        invalidClassNames = new HashSet<String>(Arrays.asList(new String[] {
            "Service"
        }));
    }
    
    protected final Map<String, String> imports = new HashMap<String, String>();
    protected final String basePackageName;
    protected final Meta meta;
    protected final Meta.Type type;
    protected final String className;
    
    public MetaConverter(String basePackageName, Meta meta, Meta.Type type) {
        this.basePackageName = basePackageName;
        this.meta = meta;
        this.type = type;
        className = getClassName(type.name);
    }
    
    public String getClassName(String typeName) {
        String[] pieces = typeName.split("_");
        // We want just the last, but add an extra piece if invalid. We don't go recursive
        //  or check top-level when going back because it's a rare occurrence and we're safe
        //  for now...
        String name = pieces[pieces.length - 1];
        if (invalidClassNames.contains(name)) {
            name = pieces[pieces.length - 2] + name;
        }
        return name;
    }
    
    public String getMethodOrPropertyName(String className, String name) {
        if (keywords.contains(name)) {
            // Prefixing a uncapitalized type name
            name = Character.toLowerCase(className.charAt(0)) + className.substring(1) +
                Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    public String getPackageName(String typeName) {
        StringBuilder pkg = new StringBuilder(basePackageName);
        if (typeName.startsWith("SoftLayer_")) {
            typeName = typeName.substring(10);
        }
        String[] pieces = typeName.split("_");
        // Skip the last one, it's the class name
        for (int i = 0; i < pieces.length - 1; i++) {
            String piece = pieces[i].toLowerCase();
            String replacement = keywordReplacements.get(piece);
            pkg.append('.').append(replacement != null ? replacement : piece);
        }
        return pkg.toString();
    }

    public TypeClass buildTypeClass() {
        imports.clear();
        String packageName = getPackageName(type.name);
        String base = null;
        Meta.Type baseMeta = null;
        String baseService = null;
        Meta.Type baseServiceMeta = null;
        if (type.base != null && !"SoftLayer_Entity".equals(type.name)) {
            String baseClassName = getClassName(type.base);
            base = getPackageName(type.base) + '.' + baseClassName;
            imports.put(baseClassName, base);
            baseMeta = meta.types.get(type.base);
            // Sometimes, the direct parent is not a service, but the grandparent is
            baseServiceMeta = baseMeta;
            if (!type.noservice) {
                while (baseServiceMeta != null && baseServiceMeta.noservice) {
                    baseServiceMeta = baseServiceMeta.base == null ? null : meta.types.get(baseServiceMeta.base);
                }
            }
            if (baseServiceMeta != null) {
                String baseServiceClassName = getClassName(baseServiceMeta.name);
                baseService = getPackageName(baseServiceMeta.name) + '.' + baseServiceClassName;
                imports.put(baseServiceClassName, baseService);
            }
        }
        
        return new TypeClass(type, imports, packageName, className, base, baseMeta,
            baseService, baseServiceMeta, getProperties(), getMethods(baseMeta));
    }
    
    public List<TypeClass.Property> getProperties() {
        List<TypeClass.Property> properties = new ArrayList<TypeClass.Property>(type.properties.size());
        for (Meta.Property property : type.properties.values()) {
            String javaType = getJavaType(property.type, property.typeArray);
            if (javaType != null) {
                properties.add(new TypeClass.Property(property, getMethodOrPropertyName(className, property.name),
                    javaType, getJavaType(property.type, false)));
                // If we are a list, we know we need the concrete impl that is used during lazily instantiation
                if (property.typeArray) {
                    imports.put("ArrayList", "java.util.ArrayList");
                }
            }
        }
        return properties;
    }
    
    public List<TypeClass.Method> getMethods(Meta.Type baseMeta) {
        List<TypeClass.Method> methods = new ArrayList<TypeClass.Method>(type.methods.size());
        for (Meta.Method method : type.methods.values()) {
            String javaType = getJavaType(method.type, method.typeArray);
            if (javaType != null) {
                boolean allParametersValid = true;
                List<TypeClass.Parameter> parameters = new ArrayList<TypeClass.Parameter>(method.parameters.size());
                for (Meta.Parameter parameter : method.parameters) {
                    String paramJavaType = getJavaType(parameter.type, parameter.typeArray);
                    if (paramJavaType == null) {
                        allParametersValid = false;
                        break;
                    }
                    parameters.add(new TypeClass.Parameter(parameter, paramJavaType));
                }
                if (allParametersValid) {
                    String name = method.name;
                    // There are some cases where one of the parent classes contains the same method with
                    //  the same parameters but with different return type. This is usually fine with our regular
                    //  interface that has regular return types which are covariant. However in the async interface
                    //  the return types are generics of future/callable which are invariant. This check makes sure
                    //  that we change the name when this happens.
                    Meta.Type parent = baseMeta;
                    while (parent != null && name.equals(method.name)) {
                        Meta.Method parentMethod = parent.methods.get(method.name);
                        if (parentMethod != null && parentMethod.parameters.size() == method.parameters.size()) {
                            // Check all parameter types. Note, parameter types are equal if they are just arrays. This
                            //  is because we use List whose type parameter is invariant too.
                            boolean parametersEqual = true;
                            for (int i = 0; i < method.parameters.size(); i++) {
                                Meta.Parameter methodParameter = method.parameters.get(i);
                                Meta.Parameter parentParameter = parentMethod.parameters.get(i);
                                parametersEqual = (methodParameter.typeArray == parentParameter.typeArray &&
                                    methodParameter.type.equals(parentMethod.type)) ||
                                    (methodParameter.typeArray && parentParameter.typeArray);
                                if (!parametersEqual) {
                                    break;
                                }
                            }
                            if (parametersEqual) {
                                // Now we know we have invariance; we have to change the Java method name. We just
                                //  append "for" + the class name. Wed don't do this check recursively, because it
                                //  is rare to resolve this ambiguity and the need hasn't arisen.
                                name += "For" + className;
                            }
                        }
                        parent = parent.base == null ? null : meta.types.get(parent.base);
                    }
                    methods.add(new TypeClass.Method(method,
                        getMethodOrPropertyName(className, name), javaType, parameters));
                }
            }
        }
        return methods;
    }
    
    public String getJavaType(String typeName, boolean array) {
    
        String javaType;
        // Attempt primitives first
        if ("base64Binary".equals(typeName)) {
            javaType = "byte[]";
        } else if ("boolean".equals(typeName)) {
            javaType = "Boolean";
        } else if ("dateTime".equals(typeName)) {
            javaType = "java.util.GregorianCalendar";
            imports.put("GregorianCalendar", javaType);
        } else if ("decimal".equals(typeName) || "float".equals(typeName)) {
            javaType = "java.math.BigDecimal";
            imports.put("BigDecimal", javaType);
        } else if ("enum".equals(typeName) || "json".equals(typeName) || "string".equals(typeName)) {
            javaType = "String";
        } else if ("int".equals(typeName) || "integer".equals(typeName) ||
                "unsignedInt".equals(typeName) || "unsignedLong".equals(typeName)) {
            javaType = "Long";
        } else if ("nonNegativeInteger".equals(typeName)) {
            javaType = "java.math.BigInteger";
            imports.put("BigInteger", javaType);
        } else if ("void".equals(typeName)) {
            javaType = "Void";
        } else if (!meta.types.containsKey(typeName)) {
            return null;
        } else {
            String className = getClassName(typeName);
            javaType = getPackageName(typeName) + '.' + className;
            imports.put(className, javaType);
        }
        if (array) {
            imports.put("List", "java.util.List");
            return "java.util.List<" + javaType + ">";
        }
        return javaType;
    }
}
