package com.softlayer.api;

/** Object holding pagination information for an API request */
public class ResultLimit {

    public final int offset;
    public final int limit;
    
    public ResultLimit(int limit) {
        this(0, limit);
    }
    
    public ResultLimit(int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }
}
