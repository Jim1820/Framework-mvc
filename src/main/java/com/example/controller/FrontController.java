package com.example.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.scanner.ControllerScanner;

public class FrontController extends HttpServlet {

    private List<String> classList = new ArrayList<>();

    @Override
    public void init() {

        System.out.println("=== INIT EXECUTEE ===");

        try {

            List<Class<?>> controllers =
                    ControllerScanner.getControllers(
                            "main.java.controller");

            System.out.println(
                    "Nombre controllers : "
                            + controllers.size());

            for (Class<?> c : controllers) {

                classList.add(c.getName());

                System.out.println(
                        "Controller trouvé : "
                                + c.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
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

    private void processRequest(HttpServletRequest req,
            HttpServletResponse resp)
            throws IOException {

        String url = req.getRequestURI();

        resp.setContentType("text/plain");

        resp.getWriter().println("URL recue : " + url);
        resp.getWriter().println();

        for (String controller : classList) {

            resp.getWriter().println(
                    "Controller trouve : "
                            + controller);
        }
    }
}