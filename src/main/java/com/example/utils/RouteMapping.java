package com.example.utils;

import java.util.HashMap;
import java.util.Map;

public class RouteMapping {

    private Map<String, Route> routes = new HashMap<>();

    public void addRoute(String url, Route route) {

        for (Route r : routes.values()) {

            if (r.equals(route)) {
                throw new RuntimeException(
                        "Route déjà déclarée : " + url);
            }
        }

        routes.put(url, route);
    }

    public Route getRoute(String url) {
        return routes.get(url);
    }

    public Map<String, Route> getRoutes() {
        return routes;
    }

}