package com.example.scanner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PackageScanner {

    public static List<Class<?>> getClasses(String packageName)
            throws Exception {

        List<Class<?>> classes = new ArrayList<>();

        String path = packageName.replace('.', '/');

        ClassLoader classLoader = PackageScanner.class.getClassLoader();
        
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            return classes;
        }

        File directory = new File(resource.toURI());

        scan(directory, packageName, classes);

        return classes;
    }

    private static void scan(
            File directory,
            String packageName,
            List<Class<?>> classes)
            throws Exception {

        File[] files = directory.listFiles();

        if (files == null)
            return;

        for (File file : files) {

            if (file.isDirectory()) {

                scan(
                        file,
                        packageName + "." + file.getName(),
                        classes);
            } else if (file.getName().endsWith(".class")) {

                String className = packageName + "."
                        + file.getName().replace(".class", "");

                classes.add(
                        Class.forName(className));
            }
        }
    }
}