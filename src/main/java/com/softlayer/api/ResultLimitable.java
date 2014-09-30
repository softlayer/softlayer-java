package com.softlayer.api;

public interface ResultLimitable {
    public ResultLimit getResultLimit();
    public ResultLimit setResultLimit(ResultLimit limit);
    public Integer getLastResponseTotalItemCount();
}
