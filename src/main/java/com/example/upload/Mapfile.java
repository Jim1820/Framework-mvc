package com.example.upload;


public class Mapfile {
    private String nom;
    private String type;      
    private byte[] contenu;   

    public Mapfile() {}

    public Mapfile(String nom, String type, byte[] contenu) {
        this.nom = nom;
        this.type = type;
        this.contenu = contenu;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public byte[] getContenu() { return contenu; }
    public void setContenu(byte[] contenu) { this.contenu = contenu; }

    public long getTaille() {
        return contenu == null ? 0 : contenu.length;
    }

    public void sauvegarder(String dossier) throws java.io.IOException {
        java.nio.file.Path chemin = java.nio.file.Paths.get(dossier, nom);
        java.nio.file.Files.createDirectories(chemin.getParent());
        java.nio.file.Files.write(chemin, contenu);
    }

}