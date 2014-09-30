package com.softlayer.api;

/** Interface for handling async method callbacks */
public interface ResponseHandler<T> {

    /** Called when the method errored. This is NOT called when onSuccess errors. */
    public void onError(Exception ex);
    
    /** Called when the method succeeds. */
    public void onSuccess(T value);
}
