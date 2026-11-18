package com.uphf.saes5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProfileSummary {
    private final List<MuscleStatistic> muscleStatistics;
    private final List<ProfileHistoryEntry> history;
    private final int starCount;

    public ProfileSummary(List<MuscleStatistic> muscleStatistics,
                         List<ProfileHistoryEntry> history, int starCount) {
        this.muscleStatistics = Collections.unmodifiableList(new ArrayList<>(muscleStatistics));
        this.history = Collections.unmodifiableList(new ArrayList<>(history));
        this.starCount = starCount;
    }

    public List<MuscleStatistic> getMuscleStatistics() { return muscleStatistics; }
    public List<ProfileHistoryEntry> getHistory() { return history; }
    public int getStarCount() { return starCount; }
}
