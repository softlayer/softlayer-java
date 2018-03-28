package com.softlayer.api;

/** Interface for handling async method callbacks */
public interface ResponseHandler<T> {

    /** Called when the method errored. This is NOT called when onSuccess errors. */
    void onError(Exception ex);
    
    /** Called when the method succeeds. */
    void onSuccess(T value);
}
