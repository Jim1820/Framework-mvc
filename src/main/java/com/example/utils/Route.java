package com.example.utils;

import java.lang.reflect.Method;

import com.example.annotation.RestApi;

public class Route {

    private final Class<?> controller;
    private final Method method;

    public Route(Class<?> controller, Method method) {
        this.controller = controller;
        this.method = method;
    }

    public Class<?> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isRestApi() {
        return controller.isAnnotationPresent(RestApi.class)
                || method.isAnnotationPresent(RestApi.class);
    }

}