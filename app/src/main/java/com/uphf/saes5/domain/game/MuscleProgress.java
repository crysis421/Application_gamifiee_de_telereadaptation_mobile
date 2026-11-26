package com.uphf.saes5.domain.game;

public enum MuscleProgress {
    TO_IMPROVE,
    PROGRESSING,
    MASTERED;

    public static MuscleProgress fromScore(int score) {
        if (score < 40) return TO_IMPROVE;
        if (score < 70) return PROGRESSING;
        return MASTERED;
    }
}
