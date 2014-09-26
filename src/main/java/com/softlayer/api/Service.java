package com.softlayer.api;

/** Interface extended by individual service interfaces on types */ 
//public interface Service<M extends Mask, A extends ServiceAsync<M>> extends Maskable<M> {
public interface Service extends Maskable {
    
    /** Get an async version of this service */
//    public A asAsync();
    public ServiceAsync asAsync();
}
