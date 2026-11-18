package com.uphf.saes5;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.annotation.RawRes;

/**
 * Retours sonores de la séance (US-3.3).
 *
 * <p>Trois sons distincts : validation, erreur, fin d'exercice. Ils guident sans obliger à
 * lire l'écran, l'utilisateur étant à plusieurs mètres pendant l'exercice.</p>
 *
 * <p>{@link SoundPool} plutôt que {@code MediaPlayer} : les sons sont courts, préchargés en
 * mémoire, et se déclenchent sans latence perceptible, y compris plusieurs fois de suite.</p>
 *
 * <p>Chaque lecture vérifie l'interrupteur « Retours sonores » (US-4.1) : couper le son en
 * cours de séance prend effet immédiatement.</p>
 */
public final class SoundFeedback {

    /** Trois sons simultanés suffisent : au-delà, les plus anciens sont coupés. */
    private static final int MAX_STREAMS = 3;

    private final SoundPool pool;
    private final int validId;
    private final int failedId;
    private final int finishId;

    public SoundFeedback(Context context) {
        pool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();

        Context app = context.getApplicationContext();
        validId = pool.load(app, R.raw.feedback_valid, 1);
        failedId = pool.load(app, R.raw.feedback_failed, 1);
        finishId = pool.load(app, R.raw.exercise_finish, 1);
    }

    /** Répétition validée. */
    public void playValid() {
        play(validId);
    }

    /** Mouvement à corriger. */
    public void playFailed() {
        play(failedId);
    }

    /** Exercice terminé. */
    public void playFinish() {
        play(finishId);
    }

    private void play(int soundId) {
        if (!GameSettings.isEnabled(GameSettings.Feature.SOUNDS)) {
            return;
        }
        // Un son encore en cours de chargement ne joue tout simplement pas : mieux vaut un
        // retour manquant au tout premier instant qu'un blocage du fil principal.
        pool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    /** À appeler quand l'écran disparaît : les sons occupent de la mémoire. */
    public void release() {
        pool.release();
    }
}
