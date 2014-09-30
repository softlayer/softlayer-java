package com.softlayer.api;

/**
 * A version of {@link ResponseHandler} that, if used, will have {@link #setLastResponseTotalItemCount(Integer)}
 * invoked after the response to set the total items count from the header. 
 */
public abstract class ResponseHandlerWithHeaders<T> implements ResponseHandler<T> {
    private Integer lastResponseTotalItemCount;
    
    public Integer getLastResponseTotalItemCount() {
        return lastResponseTotalItemCount;
    }

    public void setLastResponseTotalItemCount(Integer lastResponseTotalItemCount) {
        this.lastResponseTotalItemCount = lastResponseTotalItemCount;
    }
}
