package com.uphf.saes5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StatisticsRepository {
    private static final List<ExerciseSession> SESSIONS = Arrays.asList(
            session("squat", 18, 95), session("squat", 12, 88), session("squat", 5, 92),
            session("pompes", 17, 72), session("pompes", 10, 68), session("pompes", 3, 90),
            session("fentes", 16, 45), session("fentes", 9, 52), session("fentes", 2, 38),
            session("planche", 15, 82), session("planche", 8, 91), session("planche", 1, 87),
            session("rowing", 14, 22), session("rowing", 7, 30), session("rowing", 0, 38));

    private StatisticsRepository() {}

    public static List<ExerciseSession> getAll() {
        return new ArrayList<>(SESSIONS);
    }

    private static ExerciseSession session(String exerciseId, int daysAgo, int score) {
        long completedAt = 1_796_083_200_000L - daysAgo * 86_400_000L;
        return new ExerciseSession(exerciseId, completedAt, score);
    }
}
