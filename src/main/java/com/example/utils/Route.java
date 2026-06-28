package com.example.utils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import com.example.annotation.HttpMethod;

public class Route {

    private String url;

    private Class<?> controllerClass;
    private Method method;
    private Set<HttpMethod> httpMethods;

    public Route(String url,
            Class<?> controllerClass,
            Method method,
            Set<HttpMethod> httpMethods) {

        this.url = url;
        this.controllerClass = controllerClass;
        this.method = method;
        this.httpMethods = httpMethods;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (!(obj instanceof Route))
            return false;

        Route other = (Route) obj;

        return Objects.equals(url, other.url)
                && Objects.equals(httpMethods, other.httpMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethods);
    }
}