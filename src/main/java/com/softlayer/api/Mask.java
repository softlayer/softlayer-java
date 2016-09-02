package com.softlayer.api;

import java.util.HashMap;
import java.util.Map;

import com.softlayer.api.Property.BooleanProperty;
import com.softlayer.api.Property.ByteArrayProperty;
import com.softlayer.api.Property.DateTimeProperty;
import com.softlayer.api.Property.NumberProperty;
import com.softlayer.api.Property.StringProperty;

/** Object mask parameter. See http://sldn.softlayer.com/article/Object-Masks */
public class Mask {
    private final Map<String, Property<?>> localProperties = new HashMap<String, Property<?>>();
    private final Map<String, Mask> subMasks = new HashMap<String, Mask>();
    
    /** Clear out all previously masked objects and local properties */
    public void clear() {
        localProperties.clear();
        subMasks.clear();
    }

    private int getChildCount() {
        return localProperties.size() + subMasks.size();
    }

    protected BooleanProperty withBooleanProperty(String name) {
        Property<?> property = localProperties.get(name);
        if (property == null) {
            property = new BooleanProperty(name);
            localProperties.put(name, property);
        }
        return (BooleanProperty) property;
    }

    protected ByteArrayProperty withByteArrayProperty(String name) {
        Property<?> property = localProperties.get(name);
        if (property == null) {
            property = new ByteArrayProperty(name);
            localProperties.put(name, property);
        }
        return (ByteArrayProperty) property;
    }

    protected DateTimeProperty withDateTimeProperty(String name) {
        Property<?> property = localProperties.get(name);
        if (property == null) {
            property = new DateTimeProperty(name);
            localProperties.put(name, property);
        }
        return (DateTimeProperty) property;
    }

    protected NumberProperty withNumberProperty(String name) {
        Property<?> property = localProperties.get(name);
        if (property == null) {
            property = new NumberProperty(name);
            localProperties.put(name, property);
        }
        return (NumberProperty) property;
    }

    protected StringProperty withStringProperty(String name) {
        Property<?> property = localProperties.get(name);
        if (property == null) {
            property = new StringProperty(name);
            localProperties.put(name, property);
        }
        return (StringProperty) property;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Mask> T withSubMask(String name, Class<T> maskClass) {
        T subMask = (T) subMasks.get(name);
        if (subMask == null) {
            try {
                subMask = maskClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException();
            }
            subMasks.put(name, subMask);
        }
        return subMask;
    }

    protected String getMask() {
        return "mask[" + toString() + "]";
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /** Append this mask's string representation to the given builder and return it */
    public StringBuilder toString(StringBuilder builder) {
        boolean first = true;
        for (String localProperty : localProperties.keySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(localProperty);
        }

        for (Map.Entry<String, Mask> entry : subMasks.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(entry.getKey());

            // No count means add nothing, single is a dot, and multiple means brackets
            int count = entry.getValue().getChildCount();
            if (count == 1) {
                entry.getValue().toString(builder.append('.'));
            } else if (count > 1) {
                entry.getValue().toString(builder.append('[')).append(']');
            }
        }
        return builder;
    }

    protected Map<String, ?> getFilterMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        // Sub masks first
        for (Map.Entry<String, Mask> subMask : subMasks.entrySet()) {
            Map<String, ?> subMap = subMask.getValue().getFilterMap();
            if (!subMap.isEmpty()) {
                map.put(subMask.getKey(), subMap);
            }
        }
        // Now local properties
        for (Map.Entry<String, Property<?>> localProperty : localProperties.entrySet()) {
            Map<String, ?> localPropertyMap = localProperty.getValue().getFilterMap();
            if (!localPropertyMap.isEmpty()) {
                map.put(localProperty.getKey(), localPropertyMap);
            }
        }
        return map;
    }
}
