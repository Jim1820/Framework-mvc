package com.example.utils;

import java.io.IOException;
import java.io.InputStream;

import com.example.upload.Mapfile;

import jakarta.servlet.http.Part;

public class FileUtility {
    public static Mapfile fromPart(Part part) throws IOException {
        if (part == null || part.getSize() == 0)
            return null;

        String nom = java.nio.file.Paths.get(part.getSubmittedFileName())
                .getFileName().toString(); // évite les ../
        String type = part.getContentType();
        byte[] contenu;
        try (InputStream in = part.getInputStream()) {
            contenu = in.readAllBytes(); // Java 9+
        }
        return new Mapfile(nom, type, contenu);
    }
}
