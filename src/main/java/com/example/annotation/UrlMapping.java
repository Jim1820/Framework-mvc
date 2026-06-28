package com.example.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UrlMapping {

    String value();

    HttpMethod[] method() default {
        HttpMethod.GET,
        HttpMethod.POST
    };
}
