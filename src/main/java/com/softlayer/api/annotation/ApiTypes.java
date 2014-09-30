package com.softlayer.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.softlayer.api.service.Entity;

@Documented
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiTypes {
    
    /** Collection of every type that extends {@link Entity} no matter how deep. */
    Class<? extends Entity>[] value();
}
