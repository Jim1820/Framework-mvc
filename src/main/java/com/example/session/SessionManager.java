package com.example.session;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionManager {

    private static final String SESSION_REGISTRY =
            "MVC_SESSION_REGISTRY";

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, Session> getRegistry(
            ServletContext context) {

        synchronized (context) {

            Object registry = context.getAttribute(SESSION_REGISTRY);

            if (registry == null) {

                ConcurrentMap<String, Session> nouveauRegistre =
                        new ConcurrentHashMap<>();

                context.setAttribute(
                        SESSION_REGISTRY,
                        nouveauRegistre);

                return nouveauRegistre;
            }

            return (ConcurrentMap<String, Session>) registry;
        }
    }

    public Session getSession(HttpServletRequest request) {

        // Récupérer la session HTTP actuelle
        HttpSession httpSession = request.getSession();

        // Récupérer son identifiant
        String id = httpSession.getId();

        // Récupérer le registre partagé
        ServletContext context = request.getServletContext();

        ConcurrentMap<String, Session> registry =
                getRegistry(context);

        // Retrouver la session ou créer sa représentation
        return registry.computeIfAbsent(
                id,
                key -> new Session(httpSession));
    }

    public void removeSession(
            ServletContext context,
            String sessionId) {

        ConcurrentMap<String, Session> registry =
                getRegistry(context);

        registry.remove(sessionId);
    }
}