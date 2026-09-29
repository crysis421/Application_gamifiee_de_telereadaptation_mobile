package com.uphf.saes5;

public final class StatisticListItem {
    private final String title;
    private final String subtitle;
    private final int score;
    private final Integer perfectPercentage;
    private final int sessionCount;
    private final boolean hasData;

    public StatisticListItem(String title, String subtitle, int score,
                             Integer perfectPercentage, int sessionCount, boolean hasData) {
        this.title = title;
        this.subtitle = subtitle;
        this.score = score;
        this.perfectPercentage = perfectPercentage;
        this.sessionCount = sessionCount;
        this.hasData = hasData;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getScore() { return score; }
    public Integer getPerfectPercentage() { return perfectPercentage; }
    public int getSessionCount() { return sessionCount; }
    public boolean hasData() { return hasData; }
}
