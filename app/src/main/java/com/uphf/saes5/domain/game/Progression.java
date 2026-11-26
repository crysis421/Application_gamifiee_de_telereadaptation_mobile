package com.uphf.saes5.domain.game;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.domain.statistics.MuscleGroups;

/**
 * Rangs par groupe musculaire et étoiles par exercice (US-4.5), calculés à partir des points
 * gagnés en séance (US-4.4).
 *
 * <ul>
 *   <li>Chaque exercice gagne jusqu'à {@link #MAX_STARS} étoiles en franchissant les paliers
 *   de {@link #STAR_THRESHOLDS}.</li>
 *   <li>Chaque groupe musculaire cumule les points de ses exercices et monte de rang.</li>
 * </ul>
 *
 * <p>Les séances journalisées avant l'US-4.4 n'ont pas de points : ils sont reconstitués à
 * partir des répétitions validées et de leur précision, sans multiplicateur.</p>
 *
 * <p>Sans dépendance Android, pour rester testable.</p>
 */
public final class Progression {

    public static final int MAX_STARS = 3;
    /** Points cumulés sur un exercice pour gagner la 1re, 2e et 3e étoile. */
    public static final int[] STAR_THRESHOLDS = {300, 1_000, 2_500};

    public enum Rank {
        NOVICE(0), BRONZE(500), SILVER(2_000), GOLD(5_000), PLATINUM(10_000);

        private final int threshold;

        Rank(int threshold) {
            this.threshold = threshold;
        }

        public int getThreshold() { return threshold; }

        public static Rank forPoints(int points) {
            Rank result = NOVICE;
            for (Rank rank : values()) {
                if (points >= rank.threshold) result = rank;
            }
            return result;
        }

        /** Le rang suivant, ou {@code null} au rang maximal. */
        @Nullable
        public Rank next() {
            int index = ordinal() + 1;
            return index < values().length ? values()[index] : null;
        }
    }

    private Progression() {}

    public static int sessionPoints(SessionEntry entry) {
        if (entry.hasArcadePoints()) return entry.getArcadePoints();
        // Séance antérieure aux points : chaque succès vaut sa précision, sans multiplicateur.
        int precision = entry.hasSimilarityScore()
                ? entry.getSimilarityScore()
                : SessionEntry.SCORE_UNAVAILABLE;
        ArcadeScore replay = new ArcadeScore();
        int total = 0;
        for (int i = 0; i < entry.getValidatedReps(); i++) {
            total += replay.onSuccess(precision);
            replay.onFailure();
        }
        return total;
    }

    public static int starsFor(int points) {
        int stars = 0;
        for (int threshold : STAR_THRESHOLDS) {
            if (points >= threshold) stars++;
        }
        return Math.min(MAX_STARS, stars);
    }

    public static Summary calculate(List<Exercise> catalogue, List<SessionEntry> log) {
        Map<String, Integer> pointsByExercise = new LinkedHashMap<>();
        for (SessionEntry entry : log) {
            pointsByExercise.merge(entry.getExerciseId(), sessionPoints(entry), Integer::sum);
        }

        Map<String, Integer> pointsByMuscle = new LinkedHashMap<>();
        for (String group : MuscleGroups.ALL) pointsByMuscle.put(group, 0);

        List<ExerciseStars> exercises = new ArrayList<>();
        for (Exercise exercise : catalogue) {
            int points = pointsByExercise.getOrDefault(exercise.getId(), 0);
            exercises.add(new ExerciseStars(exercise.getId(), exercise.getName(), points));
            if (pointsByMuscle.containsKey(exercise.getMuscleGroup())) {
                pointsByMuscle.merge(exercise.getMuscleGroup(), points, Integer::sum);
            }
        }
        // Les exercices travaillés d'abord, du plus avancé au moins avancé.
        exercises.sort(Comparator.comparingInt(ExerciseStars::getPoints).reversed());

        List<MuscleRank> muscles = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pointsByMuscle.entrySet()) {
            muscles.add(new MuscleRank(entry.getKey(), entry.getValue()));
        }
        return new Summary(muscles, exercises);
    }

    public static final class Summary {
        private final List<MuscleRank> muscles;
        private final List<ExerciseStars> exercises;

        Summary(List<MuscleRank> muscles, List<ExerciseStars> exercises) {
            this.muscles = Collections.unmodifiableList(muscles);
            this.exercises = Collections.unmodifiableList(exercises);
        }

        public List<MuscleRank> getMuscles() { return muscles; }
        public List<ExerciseStars> getExercises() { return exercises; }

        public int getTotalStars() {
            int total = 0;
            for (ExerciseStars exercise : exercises) total += exercise.getStars();
            return total;
        }

        @Nullable
        public MuscleRank muscle(String group) {
            for (MuscleRank muscle : muscles) {
                if (muscle.getMuscleGroup().equals(group)) return muscle;
            }
            return null;
        }

        @Nullable
        public ExerciseStars exercise(String id) {
            for (ExerciseStars exercise : exercises) {
                if (exercise.getExerciseId().equals(id)) return exercise;
            }
            return null;
        }
    }

    public static final class MuscleRank {
        private final String muscleGroup;
        private final int points;

        MuscleRank(String muscleGroup, int points) {
            this.muscleGroup = muscleGroup;
            this.points = points;
        }

        public String getMuscleGroup() { return muscleGroup; }
        public int getPoints() { return points; }
        public Rank getRank() { return Rank.forPoints(points); }

        /** Points manquants pour le rang suivant, 0 au rang maximal. */
        public int getPointsToNextRank() {
            Rank next = getRank().next();
            return next == null ? 0 : next.getThreshold() - points;
        }
    }

    public static final class ExerciseStars {
        private final String exerciseId;
        private final String name;
        private final int points;

        ExerciseStars(String exerciseId, String name, int points) {
            this.exerciseId = exerciseId;
            this.name = name;
            this.points = points;
        }

        public String getExerciseId() { return exerciseId; }
        public String getName() { return name; }
        public int getPoints() { return points; }
        public int getStars() { return starsFor(points); }

        /** Points manquants pour l'étoile suivante, 0 quand toutes sont gagnées. */
        public int getPointsToNextStar() {
            int stars = getStars();
            return stars >= MAX_STARS ? 0 : STAR_THRESHOLDS[stars] - points;
        }
    }
}
