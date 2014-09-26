package com.softlayer.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiProperty {
    /** If set, this is the property name. Otherwise, this defaults to the field name this annotation is on. */
    String value() default "";
    
    /**
     * If set and true, the value for this property can be null or unset. All properties with these ambiguous
     * states must have a sibling field that is the same name suffixed with "Specified" that is a boolean.
     * If the boolean is true, the property is considered set to null and if it is false the property is not
     * considered set and won't be serialized.
     */
    boolean canBeNullOrNotSet() default false;
}
