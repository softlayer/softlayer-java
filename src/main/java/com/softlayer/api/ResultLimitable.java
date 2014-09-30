package com.softlayer.api;

/** Interface implemented by services to support pagination */
public interface ResultLimitable {
    
    public ResultLimit getResultLimit();
    
    public ResultLimit setResultLimit(ResultLimit limit);
    
    /** The non-paginated total item count. This can be overwritten if a service is reused */ 
    public Integer getLastResponseTotalItemCount();
}
