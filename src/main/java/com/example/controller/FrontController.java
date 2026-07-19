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
import java.util.Map;

import com.example.annotation.HttpMethod;
import com.example.annotation.UrlMapping;
import com.example.modelview.ModelView;
import com.example.scanner.ControllerScanner;

import com.example.utils.Route;
import com.example.utils.RouteMapping;

public class FrontController extends HttpServlet {

    private List<Class<?>> classList = new ArrayList<>();
    private RouteMapping routeMapping = new RouteMapping();
    private String viewPrefix;
    private String viewSuffix;

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

    public void setMapping() {

        for (Class<?> clazz : classList) {

            for (Method method : clazz.getDeclaredMethods()) {

                if (!method.isAnnotationPresent(UrlMapping.class)) {
                    continue;
                }

                UrlMapping annotation = method.getAnnotation(UrlMapping.class);

                for (HttpMethod httpMethod : annotation.method()) {

                    routeMapping.addRoute(
                            annotation.value(),
                            httpMethod,
                            clazz,
                            method);
                }
            }
        }
    }

    @Override
    public void init() throws ServletException {

        super.init();

        viewPrefix = getServletConfig().getInitParameter("view-prefix");
        viewSuffix = getServletConfig().getInitParameter("view-suffix");

        setClassList("main.java.controller");
        setMapping();
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

        String url = getRelativeUrl(req);

        HttpMethod requestMethod = HttpMethod.valueOf(req.getMethod());

        Route mappedRoute = routeMapping.getRoute(url, requestMethod);
        if (mappedRoute == null) {
            resp.setContentType("text/plain");
            resp.getWriter().println("ContextPath : " + req.getContextPath());
            resp.getWriter().println("Méthode HTTP : " + req.getMethod());
            resp.getWriter().println("URL recue : " + url);
            resp.getWriter().println();

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

            PrintWriter out = resp.getWriter();

            out.println("Aucune méthode trouvée pour : " + url);

            for (String supportedUrl : routeMapping.getRoutes().keySet()) {
                out.println("<li>" + supportedUrl + "</li>");
            }

        } else {
            try {
                Object controller = mappedRoute.getController()
                        .getDeclaredConstructor()
                        .newInstance();

                Object result = mappedRoute.getMethod().invoke(controller);
                if (result instanceof ModelView) {

                    ModelView mv = (ModelView) result;

                    for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {

                        req.setAttribute(
                                entry.getKey(),
                                entry.getValue());

                    }

                    System.out.println("Vue = " + mv.getView());

                    String path = resolve(mv.getView());

                    System.out.println("Path = " + path);

                    System.out.println(req.getServletContext().getRealPath(path));

                    req.getRequestDispatcher(path)
                            .forward(req, resp);
                } else {
                    resp.getWriter().println("URL : " + url);
                    resp.getWriter().println(
                            "Méthode Java : " + mappedRoute.getMethod().getName());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private String getRelativeUrl(HttpServletRequest req) {

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();

        String relativeUrl = requestURI.substring(contextPath.length());

        return relativeUrl.isEmpty() ? "/" : relativeUrl;
    }

    public String resolve(String viewName) {
        return viewPrefix + viewName + viewSuffix;
    }

}