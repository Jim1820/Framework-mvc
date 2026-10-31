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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.example.annotation.Auth;
import com.example.annotation.HttpMethod;
import com.example.annotation.UrlMapping;
import com.example.modelview.ModelView;
import com.example.scanner.ControllerScanner;
import com.example.upload.Mapfile;
import com.example.utils.Route;
import com.example.utils.RouteMapping;
import com.example.utils.FrameworkContextListener;
import com.example.utils.JsonConverter;
import com.example.annotation.Param;
import com.example.session.Session;
import com.example.session.SessionListener;

public class FrontController extends HttpServlet {

    private List<Class<?>> classList = new ArrayList<>();
    private RouteMapping routeMapping = new RouteMapping();
    private String viewPrefix;
    private String viewSuffix;
    private JsonConverter jsonConverter;
    private ConcurrentMap<String, Session> sessionRegistry;
    private String authAttribute;
    private String roleAttribute;
    private final Map<Class<?>, Object> controllerInstances = new HashMap<>();

    public void setClassList(String packageName) throws ServletException {
        System.out.println("=== INIT EXECUTEE ===");

        try {
            List<Class<?>> controllers = ControllerScanner.getControllers(packageName);

            if (controllers.isEmpty()) {
                throw new ServletException(
                        "Aucun contrôleur trouvé dans le package : " + packageName);
            }

            System.out.println("Nombre controllers : " + controllers.size());

            for (Class<?> c : controllers) {

                classList.add(c);

                // Une seule instance par contrôleur, créée au démarrage
                controllerInstances.put(
                        c,
                        c.getDeclaredConstructor().newInstance());

                System.out.println("Controller trouvé : " + c.getName());
            }

        } catch (NoSuchMethodException e) {
            throw new ServletException(
                    "Un contrôleur doit avoir un constructeur public sans argument : "
                            + e.getMessage(),
                    e);

        } catch (Exception e) {
            throw new ServletException(
                    "Erreur lors du scan ou de l'instanciation des contrôleurs : "
                            + e.getMessage(),
                    e);
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

    private boolean checkAuth(Method method,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        Auth auth = method.getAnnotation(Auth.class);

        // Méthode non protégée : accès libre
        if (auth == null) {
            return true;
        }

        // false = ne crée pas de session si elle n'existe pas
        HttpSession httpSession = req.getSession(false);

        if (httpSession == null || httpSession.getAttribute(authAttribute) == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentification requise pour accéder à cette ressource.");
            return false;
        }

        String[] rolesAutorises = auth.roles();

        if (rolesAutorises.length > 0) {
            Object roleUtilisateur = httpSession.getAttribute(roleAttribute);

            boolean autorise = roleUtilisateur != null
                    && Arrays.asList(rolesAutorises).contains(roleUtilisateur.toString());

            if (!autorise) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Accès refusé : rôle insuffisant.");
                return false;
            }
        }

        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {

        super.init();
        jsonConverter = new JsonConverter();
        viewPrefix = getServletConfig().getInitParameter("view-prefix");
        viewSuffix = getServletConfig().getInitParameter("view-suffix");

        if (viewPrefix == null || viewSuffix == null) {
            throw new ServletException(
                    "Paramètres 'view-prefix' et 'view-suffix' manquants dans web.xml");
        }

        Object registry = getServletContext()
                .getAttribute(SessionListener.SESSION_REGISTRY);

        authAttribute = (String) getServletContext()
                .getAttribute(FrameworkContextListener.AUTH_ATTRIBUTE);
        roleAttribute = (String) getServletContext()
                .getAttribute(FrameworkContextListener.ROLE_ATTRIBUTE);

        if (authAttribute == null || roleAttribute == null) {
            throw new ServletException(
                    "FrameworkContextListener n'a pas été exécuté au démarrage.");
        }

        if (registry == null) {
            throw new ServletException(
                    "Registre de sessions non initialisé : "
                            + "SessionListener n'a pas été exécuté au démarrage.");
        }

        sessionRegistry = (ConcurrentMap<String, Session>) registry;

        // 2. Scanner et instancier les contrôleurs (lève ServletException si invalide)
        setClassList("main.java.controller");

        // 3. Construire les routes (route en double = échec au démarrage)
        try {
            setMapping();
        } catch (IllegalStateException e) {
            throw new ServletException(
                    "Erreur de mapping des routes : " + e.getMessage(), e);
        }
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

    private Session getSession(HttpServletRequest req) {

        HttpSession httpSession = req.getSession();

        return sessionRegistry.computeIfAbsent(
                httpSession.getId(),
                key -> new Session(httpSession));
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
            // 1. Session personnalisée du framework
            if (parameterType.equals(Session.class)) {

                arguments[i] = getSession(req); // avant : sessionManager.getSession(req)

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
            // 1. Récupérer la méthode d'action
            Method method = mappedRoute.getMethod();

            // 2. Contrôle d'authentification (@Auth)
            if (!checkAuth(method, req, resp)) {
                return; // erreur 401/403 déjà envoyée
            }

            // 3. Récupérer l'instance singleton du contrôleur
            Object controller = controllerInstances.get(mappedRoute.getController());

            if (controller == null) {
                throw new IllegalStateException(
                        "Aucune instance pour le contrôleur : "
                                + mappedRoute.getController().getName());
            }

            // 4. Préparer les paramètres de la méthode
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
                    req.setAttribute(entry.getKey(), entry.getValue());
                }

                System.out.println("Vue = " + mv.getView());

                String path = resolve(mv.getView());

                System.out.println("Path = " + path);

                System.out.println(req.getServletContext().getRealPath(path));

                req.getRequestDispatcher(path).forward(req, resp);

            } else if (result != null) {

                resp.getWriter().println("URL : " + url);
                resp.getWriter().println("Méthode Java : " + method.getName());
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