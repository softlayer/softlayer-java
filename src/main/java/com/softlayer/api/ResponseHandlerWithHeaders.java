package com.softlayer.api;

public abstract class ResponseHandlerWithHeaders<T> implements ResponseHandler<T> {
    private Integer lastResponseTotalItemCount;
    
    public Integer getLastResponseTotalItemCount() {
        return lastResponseTotalItemCount;
    }

    public void setLastResponseTotalItemCount(Integer lastResponseTotalItemCount) {
        this.lastResponseTotalItemCount = lastResponseTotalItemCount;
    }
}
