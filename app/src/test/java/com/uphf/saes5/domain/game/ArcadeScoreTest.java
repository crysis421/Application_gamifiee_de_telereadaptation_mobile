package com.uphf.saes5.domain.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.uphf.saes5.domain.model.SessionEntry;

/** Points et multiplicateurs du mode arcade (US-4.4). */
public class ArcadeScoreTest {

    @Test
    public void startsAtZero_withoutMultiplier() {
        ArcadeScore score = new ArcadeScore();

        assertEquals(0, score.getTotal());
        assertEquals(1, score.getMultiplier());
    }

    @Test
    public void pointsGrowWithPrecision() {
        int sloppy = new ArcadeScore().onSuccess(60);
        int precise = new ArcadeScore().onSuccess(90);

        assertTrue(precise > sloppy);
        assertEquals(60, sloppy);
        assertEquals(90, precise);
    }

    @Test
    public void aPerfectMovement_earnsABonus() {
        int nearlyPerfect = new ArcadeScore().onSuccess(94);
        int perfect = new ArcadeScore().onSuccess(ArcadeScore.PERFECT_PRECISION);

        assertEquals(ArcadeScore.PERFECT_PRECISION + ArcadeScore.PERFECT_BONUS, perfect);
        assertTrue(perfect - nearlyPerfect > ArcadeScore.PERFECT_BONUS - 2);
    }

    @Test
    public void consecutiveSuccesses_raiseTheMultiplier() {
        ArcadeScore score = new ArcadeScore();
        for (int i = 0; i < ArcadeScore.SUCCESSES_PER_LEVEL; i++) score.onSuccess(80);

        assertEquals(2, score.getMultiplier());
        // La répétition suivante est payée double.
        assertEquals(160, score.onSuccess(80));
    }

    @Test
    public void theMultiplierIsCapped() {
        ArcadeScore score = new ArcadeScore();
        for (int i = 0; i < 50; i++) score.onSuccess(80);

        assertEquals(ArcadeScore.MAX_MULTIPLIER, score.getMultiplier());
    }

    @Test
    public void aFailure_resetsTheMultiplier_butKeepsThePoints() {
        ArcadeScore score = new ArcadeScore();
        for (int i = 0; i < 6; i++) score.onSuccess(80);
        int before = score.getTotal();

        score.onFailure();

        assertEquals(1, score.getMultiplier());
        assertEquals(0, score.getStreak());
        assertEquals(before, score.getTotal());
    }

    @Test
    public void withoutMeasuredPrecision_eachSuccessEarnsTheBase() {
        // Séance manuelle : pas de score de similarité.
        assertEquals(ArcadeScore.BASE_POINTS,
                new ArcadeScore().onSuccess(SessionEntry.SCORE_UNAVAILABLE));
    }

    @Test
    public void totalAddsUpEveryGain() {
        ArcadeScore score = new ArcadeScore();
        int sum = 0;
        for (int i = 0; i < 7; i++) sum += score.onSuccess(85);

        assertEquals(sum, score.getTotal());
    }
}
