package com.softlayer.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMethod {
    /** If provided, this is the name of the method. Otherwise it is the name of the method it is on. */
    String value() default "";
    
    /** If provided and true, this method can only be invoked on a service with an identifier */
    boolean instanceRequired() default false;
}
