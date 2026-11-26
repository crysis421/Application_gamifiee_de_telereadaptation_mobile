package com.uphf.saes5.data.session;

import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.statistics.ScoredSession;
import com.uphf.saes5.domain.statistics.StatisticsExercise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Données des écrans Statistiques et Profil, tirées des vraies séances.
 *
 * <p>Jusqu'ici ce dépôt contenait quinze séances écrites en dur : l'écran affichait les mêmes
 * chiffres quoi que fasse l'utilisateur. Il lit désormais le journal des séances
 * ({@link SessionLog}, US-7.1) et le catalogue ({@link ExerciseRepository}). Il n'y a donc
 * plus qu'une source de vérité : une séance terminée apparaît partout.</p>
 *
 * <p>Les conversions sont statiques et sans Android, pour être testées hors émulateur.</p>
 */
public final class StatisticsRepository {

    private StatisticsRepository() {}

    public static List<StatisticsExercise> getExercises() {
        return toStatisticsExercises(ExerciseRepository.getAll(), SessionLog.readAll());
    }

    public static List<ScoredSession> getAll() {
        return toStatisticsSessions(SessionLog.readAll());
    }

    /**
     * Les exercices du catalogue, puis ceux qui n'existent plus mais ont des séances : supprimer
     * un exercice ne doit pas effacer le travail déjà fait. Ces derniers n'ont plus de groupe
     * musculaire connu et ne comptent donc dans aucun muscle.
     */
    static List<StatisticsExercise> toStatisticsExercises(List<Exercise> catalogue,
                                                          List<SessionEntry> log) {
        Map<String, StatisticsExercise> byId = new LinkedHashMap<>();
        for (Exercise exercise : catalogue) {
            byId.put(exercise.getId(), new StatisticsExercise(
                    exercise.getId(), exercise.getName(), exercise.getMuscleGroup()));
        }
        for (SessionEntry entry : log) {
            if (!byId.containsKey(entry.getExerciseId())) {
                byId.put(entry.getExerciseId(), new StatisticsExercise(
                        entry.getExerciseId(), entry.getExerciseName(), ""));
            }
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * Une séance journalisée devient une séance de statistiques.
     *
     * <p>La qualité est le score de similarité quand la caméra l'a mesuré, sinon le taux de
     * réussite. Une séance sans aucun mouvement tenté est écartée : l'ouvrir puis la quitter
     * ne doit pas faire baisser les moyennes.</p>
     */
    static List<ScoredSession> toStatisticsSessions(List<SessionEntry> log) {
        List<ScoredSession> sessions = new ArrayList<>();
        for (SessionEntry entry : log) {
            if (entry.getTotalAttempts() == 0 && !entry.hasSimilarityScore()) continue;
            sessions.add(new ScoredSession(
                    entry.getExerciseId(), entry.getTimestampMillis(), qualityOf(entry)));
        }
        return sessions;
    }

    static int qualityOf(SessionEntry entry) {
        return entry.hasSimilarityScore() ? entry.getSimilarityScore() : entry.getSuccessRate();
    }
}
