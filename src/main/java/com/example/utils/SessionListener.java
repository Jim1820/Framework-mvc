package com.example.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.concurrent.ConcurrentMap;

@WebListener
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {

        ServletContext context =
                event.getSession().getServletContext();

        Object registryObject =
                context.getAttribute("MVC_SESSION_REGISTRY");

        if (registryObject instanceof ConcurrentMap<?, ?> registry) {

            registry.remove(event.getSession().getId());
        }
    }
}