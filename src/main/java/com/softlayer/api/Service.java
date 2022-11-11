package com.softlayer.api;

/** Interface extended by individual service interfaces on types */
public interface Service extends Maskable, Filterable, ResultLimitable {
    
    /** Get an async version of this service */
    ServiceAsync asAsync();
}
