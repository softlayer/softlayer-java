package com.softlayer.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Type {
    
    protected Map<String, Object> unknownProperties;
    
    /**
     * Get all unknown properties (or an empty map). The result of mutating the resulting
     * map is undefined and may result in an error.
     */
    public Map<String, Object> getUnknownProperties() {
        if (unknownProperties == null) {
            return Collections.emptyMap();
        }
        return unknownProperties;
    }
    
    /**
     * Set the unknown properties for this type. The values are copied to an immutable map.
     * Note, these values are NOT serialized into the type.
     */
    public void setUnknownProperties(Map<String, Object> unknownProperties) {
        this.unknownProperties = Collections.unmodifiableMap(
            new HashMap<String, Object>(unknownProperties));
    }
}
