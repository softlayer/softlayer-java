package com.softlayer.api;

public interface Service<M extends Mask, A extends ServiceAsync<M>> extends Maskable<M> {
    public A asAsync();
}
