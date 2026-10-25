package com.example.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.annotation.HttpMethod;
import com.example.annotation.UrlMapping;
import com.example.modelview.ModelView;
import com.example.scanner.ControllerScanner;
import com.example.session.SessionManager;
import com.example.upload.Mapfile;
import com.example.utils.Route;
import com.example.utils.RouteMapping;
import com.example.utils.JsonConverter;
import com.example.annotation.Param;
import com.example.session.Session;

public class FrontController extends HttpServlet {

    private List<Class<?>> classList = new ArrayList<>();
    private RouteMapping routeMapping = new RouteMapping();
    private String viewPrefix;
    private String viewSuffix;
    private JsonConverter jsonConverter;
    private final SessionManager sessionManager = new SessionManager();

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
        jsonConverter = new JsonConverter();
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

    public class RequestData {

        private Map<String, String> parameters;

        public RequestData(HttpServletRequest request) {

            parameters = new HashMap<>();

            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    parameters.put(key, values[0]);
                }
            });
        }

        public String get(String name) {
            return parameters.get(name);
        }

        public Map<String, String> getAll() {
            return parameters;
        }
    }

    private Object resolveParamValue(
            HttpServletRequest req,
            String paramName,
            Class<?> parameterType) throws Exception {

        // 1. Gestion des fichiers envoyés par formulaire
        if (parameterType.equals(Mapfile.class)) {

            Part part = req.getPart(paramName);

            if (part == null
                    || part.getSubmittedFileName() == null
                    || part.getSubmittedFileName().isBlank()
                    || part.getSize() == 0) {
                return null;
            }

            // Récupérer uniquement le nom du fichier
            String nom = java.nio.file.Paths
                    .get(part.getSubmittedFileName())
                    .getFileName()
                    .toString();

            String type = part.getContentType();

            byte[] contenu;

            try (InputStream input = part.getInputStream()) {
                contenu = input.readAllBytes();
            }

            // Construire l'objet Mapfile
            return new Mapfile(nom, type, contenu);
        }

        // 2. Gestion des paramètres HTTP classiques
        String value = req.getParameter(paramName);

        if (value == null) {
            return null;
        }

        if (parameterType.equals(String.class)) {
            return value;
        }

        if (parameterType.equals(int.class)
                || parameterType.equals(Integer.class)) {
            return Integer.valueOf(value);
        }

        if (parameterType.equals(long.class)
                || parameterType.equals(Long.class)) {
            return Long.valueOf(value);
        }

        if (parameterType.equals(double.class)
                || parameterType.equals(Double.class)) {
            return Double.valueOf(value);
        }

        if (parameterType.equals(boolean.class)
                || parameterType.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        }

        throw new IllegalArgumentException(
                "Type non pris en charge : "
                        + parameterType.getName());
    }

    private Object[] resolveParameters(
            Method method,
            HttpServletRequest req) throws Exception {

        Parameter[] parameters = method.getParameters();

        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {

            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();

            // 1. Session personnalisée du framework
            if (parameterType.equals(Session.class)) {

                arguments[i] = sessionManager.getSession(req);

            }

            // 2. Session HTTP native
            else if (parameterType.equals(HttpSession.class)) {

                arguments[i] = req.getSession();

            }

            // 3. Paramètres annotés avec @Param
            else if (parameter.isAnnotationPresent(Param.class)) {

                Param annotation = parameter.getAnnotation(Param.class);

                String paramName = annotation.value();
                
                arguments[i] = resolveParamValue(
                        req,
                        paramName,
                        parameterType);
            }

            // 4. Paramètre non pris en charge
            else {

                throw new IllegalArgumentException(
                        "Paramètre non pris en charge : "
                                + parameter.getName()
                                + " dans la méthode "
                                + method.getName());
            }
        }

        return arguments;
    }

    private void processRequest(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        String url = getRelativeUrl(req);

        HttpMethod requestMethod = HttpMethod.valueOf(req.getMethod());

        Route mappedRoute = routeMapping.getRoute(url, requestMethod);

        if (mappedRoute == null) {

            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");

            resp.getWriter().println("ContextPath : " + req.getContextPath());
            resp.getWriter().println("Méthode HTTP : " + req.getMethod());
            resp.getWriter().println("URL recue : " + url);
            resp.getWriter().println();

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

            PrintWriter out = resp.getWriter();

            out.println("Aucune méthode trouvée pour : " + url);

            for (String supportedUrl : routeMapping.getRoutes().keySet()) {
                out.println(supportedUrl);
            }

            return;
        }

        try {
            // 1. Créer une instance du contrôleur
            Object controller = mappedRoute.getController()
                    .getDeclaredConstructor()
                    .newInstance();

            // 2. Récupérer la méthode d'action
            Method method = mappedRoute.getMethod();

            // 3. Préparer les paramètres de la méthode
            Object[] arguments = resolveParameters(method, req);

            Object result = method.invoke(controller, arguments);

            // 5. Traiter le résultat
            if (mappedRoute.isRestApi()) {

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");

                String json = jsonConverter.toJson(result);

                resp.getWriter().write(json);

            } else if (result instanceof ModelView) {

                ModelView mv = (ModelView) result;

                for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {

                    req.setAttribute(
                            entry.getKey(),
                            entry.getValue());
                }

                System.out.println("Vue = " + mv.getView());

                String path = resolve(mv.getView());

                System.out.println("Path = " + path);

                System.out.println(
                        req.getServletContext().getRealPath(path));

                req.getRequestDispatcher(path).forward(req, resp);

            } else if (result != null) {

                resp.getWriter().println("URL : " + url);

                resp.getWriter().println(
                        "Méthode Java : " + method.getName());
            }

        } catch (Exception e) {

            e.printStackTrace();

            if (!resp.isCommitted()) {
                resp.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Erreur lors de l'exécution de la méthode d'action.");
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