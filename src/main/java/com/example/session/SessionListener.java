package com.example.session;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@WebListener
public class SessionListener implements ServletContextListener, HttpSessionListener {

    public static final String SESSION_REGISTRY = "MVC_SESSION_REGISTRY";

    // Démarrage de l'application : création du registre (une seule fois)
    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(
                SESSION_REGISTRY,
                new ConcurrentHashMap<String, Session>());
    }

    // Arrêt de l'application
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(SESSION_REGISTRY);
    }

    // Expiration ou invalidate() d'une session HTTP
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        ServletContext context = event.getSession().getServletContext();
        Object registryObject = context.getAttribute(SESSION_REGISTRY);

        if (registryObject instanceof ConcurrentMap<?, ?> registry) {
            registry.remove(event.getSession().getId());
        }
    }
}