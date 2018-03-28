package com.softlayer.api;

/** Interface implemented by services to support pagination */
public interface ResultLimitable {
    
    ResultLimit getResultLimit();
    
    ResultLimit setResultLimit(ResultLimit limit);
    
    /** The non-paginated total item count. This can be overwritten if a service is reused */ 
    Integer getLastResponseTotalItemCount();
}
