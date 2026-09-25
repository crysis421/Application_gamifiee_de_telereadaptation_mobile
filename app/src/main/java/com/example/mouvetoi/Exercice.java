package com.example.mouvetoi;

/**
 * Représente un exercice tel que défini dans US-2.1.
 */
public class Exercice {

    private String nom;
    private String description;
    private String groupeMusculaire;
    private String materiel;
    private String difficulte;
    private int dureeEstimeeMinutes;

    public Exercice(String nom, String description, String groupeMusculaire,
                     String materiel, String difficulte, int dureeEstimeeMinutes) {
        this.nom = nom;
        this.description = description;
        this.groupeMusculaire = groupeMusculaire;
        this.materiel = materiel;
        this.difficulte = difficulte;
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
    }

    public String getNom() {
        return nom;
    }

    public String getDescription() {
        return description;
    }

    public String getGroupeMusculaire() {
        return groupeMusculaire;
    }

    public String getMateriel() {
        return materiel;
    }

    public String getDifficulte() {
        return difficulte;
    }

    public int getDureeEstimeeMinutes() {
        return dureeEstimeeMinutes;
    }
}
