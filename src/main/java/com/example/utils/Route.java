package com.example.utils;

import java.lang.reflect.Method;

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
}