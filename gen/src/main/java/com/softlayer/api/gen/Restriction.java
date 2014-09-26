package com.softlayer.api.gen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Restriction {
    /** If empty, there is no restriction on any type */
    public final Set<String> types = new HashSet<String>();
    
    /** If empty, there is no restriction on any property */
    public final Map<String, Set<String>> properties = new HashMap<String, Set<String>>();
    
    /** If empty, there is no restriction on any method */
    public final Map<String, Set<String>> methods = new HashMap<String, Set<String>>();
}
