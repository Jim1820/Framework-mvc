package com.example.utils;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.example.annotation.HttpMethod;

public class RouteMapping {

    private final Map<String, EnumMap<HttpMethod, Route>> routes = new HashMap<>();

    public void addRoute(String url,
                         HttpMethod httpMethod,
                         Class<?> controller,
                         Method method) {

        EnumMap<HttpMethod, Route> methodRoutes =
                routes.computeIfAbsent(url, u -> new EnumMap<>(HttpMethod.class));

        if (methodRoutes.containsKey(httpMethod)) {
            throw new IllegalStateException(
                    "La route " + httpMethod + " " + url + " existe déjà."
            );
        }

        methodRoutes.put(
                httpMethod,
                new Route(controller, method)
        );
    }

    public Route getRoute(String url, HttpMethod httpMethod) {

        EnumMap<HttpMethod, Route> methodRoutes = routes.get(url);

        if (methodRoutes == null) {
            return null;
        }

        return methodRoutes.get(httpMethod);
    }

    public Map<String, EnumMap<HttpMethod, Route>> getRoutes() {
        return routes;
    }
}