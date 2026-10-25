package com.example.session;

import jakarta.servlet.http.HttpSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Session {

    private final HttpSession httpSession;

    private final Map<String, Object> attributs = new HashMap<>();

    public Session(HttpSession httpSession) {
        this.httpSession = httpSession;

        // Charger les attributs déjà présents dans la session HTTP
        synchroniserDepuisHttpSession();
    }

    public String getId() {
        return httpSession.getId();
    }

    public Object getAttribute(String key) {
        return httpSession.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {

        if (value == null) {
            removeAttribute(key);
            return;
        }

        httpSession.setAttribute(key, value);
        attributs.put(key, value);
    }

    public void removeAttribute(String key) {
        httpSession.removeAttribute(key);
        attributs.remove(key);
    }

    public Map<String, Object> getAttributs() {
        synchroniserDepuisHttpSession();

        return Collections.unmodifiableMap(
                new HashMap<>(attributs));
    }

    private void synchroniserDepuisHttpSession() {

        attributs.clear();

        var noms = httpSession.getAttributeNames();

        while (noms.hasMoreElements()) {
            String nom = noms.nextElement();

            attributs.put(
                    nom,
                    httpSession.getAttribute(nom));
        }
    }

    public void invalidate() {
        attributs.clear();
        httpSession.invalidate();
    }
}