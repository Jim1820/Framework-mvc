package com.example.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class FrameworkContextListener implements ServletContextListener {

    public static final String AUTH_ATTRIBUTE = "FRAMEWORK_AUTH_ATTRIBUTE";
    public static final String ROLE_ATTRIBUTE = "FRAMEWORK_ROLE_ATTRIBUTE";

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext context = event.getServletContext();

        String authAttribute = context.getInitParameter("auth.session.attribute");
        String roleAttribute = context.getInitParameter("auth.role.attribute");

        if (authAttribute == null || authAttribute.isBlank()) {
            throw new IllegalStateException(
                    "Paramètre 'auth.session.attribute' manquant dans web.xml");
        }

        if (roleAttribute == null || roleAttribute.isBlank()) {
            throw new IllegalStateException(
                    "Paramètre 'auth.role.attribute' manquant dans web.xml");
        }

        context.setAttribute(AUTH_ATTRIBUTE, authAttribute.trim());
        context.setAttribute(ROLE_ATTRIBUTE, roleAttribute.trim());

        System.out.println("[Framework] Auth : attribut = " + authAttribute
                + ", rôle = " + roleAttribute);
    }
}