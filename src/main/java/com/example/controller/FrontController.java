package com.example.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.example.annotation.UrlMapping;
import com.example.scanner.ControllerScanner;

import com.example.utils.Route;
import com.example.utils.RouteMapping;

public class FrontController extends HttpServlet {

    private List<Class<?>> classList = new ArrayList<>();
    private RouteMapping routeMapping = new RouteMapping();

    public void setClassList(String packageName) {
        System.out.println("=== INIT EXECUTEE ===");

        try {

            List<Class<?>> controllers = ControllerScanner.getControllers(
                    packageName);

            System.out.println(
                    "Nombre controllers : "
                            + controllers.size());

            for (Class<?> c : controllers) {

                classList.add(c);

                System.out.println(
                        "Controller trouvé : "
                                + c.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SetMapping() throws Exception {

        for (Class<?> classes : classList) {

            for (Method method : classes.getDeclaredMethods()) {

                if (method.isAnnotationPresent(UrlMapping.class)) {

                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);

                    Route route = new Route(
                            annotation.value(),
                            classes,
                            method,
                            Set.of(annotation.method()));

                    routeMapping.addRoute(annotation.value(), route);
                }
            }
        }
    }

    @Override
    public void init() {
        setClassList("main.java.controller");
    }

    @Override
    protected void doGet(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {

        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {

        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req,
            HttpServletResponse resp)
            throws IOException {

        String url = req.getRequestURI();

        resp.setContentType("text/plain");

        resp.getWriter().println("URL recue : " + url);
        resp.getWriter().println();

        for (Class controller : classList) {

            resp.getWriter().println(
                    "Controller trouve : "
                            + controller.getName());
        }
        try {
            SetMapping();
        } catch (Exception e) {
            resp.getWriter().println(e.getMessage());
        }
        
        Route route = routeMapping.getRoute(url);

        if (route == null) {

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

            PrintWriter out = resp.getWriter();

            out.println("<h2>404 - URL non supportée</h2>");
            out.println("<p>L'URL <b>" + url + "</b> n'existe pas.</p>");

            out.println("<h3>URLs disponibles :</h3>");
            out.println("<ul>");

            for (String supportedUrl : routeMapping.getRoutes().keySet()) {
                out.println("<li>" + supportedUrl + "</li>");
            }

            out.println("</ul>");

        }
    }

}