package com.softlayer.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Type {
    
    protected Map<String, Object> unknownProperties;
    
    public Map<String, Object> getUnknownProperties() {
        if (unknownProperties == null) {
            return Collections.emptyMap();
        }
        return unknownProperties;
    }
    
    public void setUnknownProperties(Map<String, Object> unknownProperties) {
        this.unknownProperties = Collections.unmodifiableMap(
            new HashMap<String, Object>(unknownProperties));
    }

}
