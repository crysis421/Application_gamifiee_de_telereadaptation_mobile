package com.uphf.saes5.accueil.model;

/**
 * Résultat d'une session d'exercice : ce qui est affiché sur l'écran de récapitulatif et ce qui
 * devra être journalisé pour la recherche (US-7.1).
 *
 * <p>Classe sans dépendance Android pour rester testable en test unitaire local.</p>
 */
public class ExerciseSession {

    private final String exerciseId;
    private final int validatedReps;
    private final int failedReps;
    private final long durationMillis;

    public ExerciseSession(String exerciseId, int validatedReps, int failedReps, long durationMillis) {
        this.exerciseId = exerciseId;
        this.validatedReps = validatedReps;
        this.failedReps = failedReps;
        this.durationMillis = durationMillis;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public int getValidatedReps() {
        return validatedReps;
    }

    public int getFailedReps() {
        return failedReps;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    /** Nombre total de mouvements tentés (validés + ratés). */
    public int getTotalAttempts() {
        return validatedReps + failedReps;
    }

    /**
     * Pourcentage de réussite, arrondi à l'entier le plus proche.
     * Vaut 0 si aucun mouvement n'a été tenté.
     */
    public int getSuccessRate() {
        int attempts = getTotalAttempts();
        if (attempts == 0) {
            return 0;
        }
        return Math.round((validatedReps * 100f) / attempts);
    }

    /** Durée moyenne d'une répétition validée, en millisecondes. 0 si aucune répétition. */
    public long getAverageMillisPerRep() {
        if (validatedReps == 0) {
            return 0L;
        }
        return durationMillis / validatedReps;
    }

    /** Indique si l'objectif de répétitions de l'exercice est atteint. */
    public boolean isGoalReached(int targetReps) {
        return targetReps > 0 && validatedReps >= targetReps;
    }
}
