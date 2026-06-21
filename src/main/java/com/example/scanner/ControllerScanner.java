package com.example.scanner;

import java.util.ArrayList;
import java.util.List;

import com.example.annotation.Controller;

public class ControllerScanner {

    public static List<Class<?>> getControllers(String packageName)
            throws Exception {

        List<Class<?>> controllers = new ArrayList<>();

        List<Class<?>> classes =
                PackageScanner.getClasses(packageName);

        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Controller.class)) {
                controllers.add(clazz);
            }
        }

        return controllers;
    }
}