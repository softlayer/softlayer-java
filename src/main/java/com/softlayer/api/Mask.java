package com.softlayer.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Object mask parameter. See http://sldn.softlayer.com/article/Object-Masks */
public class Mask {
    private final Set<String> localProperties = new HashSet<>();
    private final Map<String, Mask> subMasks = new HashMap<>();
    
    /** Clear out all previously masked objects and local properties */
    public void clear() {
        localProperties.clear();
        subMasks.clear();
    }

    private int getChildCount() {
        return localProperties.size() + subMasks.size();
    }

    protected void withLocalProperty(String localProperty) {
        localProperties.add(localProperty);
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
        for (String localProperty : localProperties) {
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
}
