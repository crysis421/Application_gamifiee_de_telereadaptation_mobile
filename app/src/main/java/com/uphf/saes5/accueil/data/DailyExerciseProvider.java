package com.uphf.saes5.accueil.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.uphf.saes5.Exercise;
import com.uphf.saes5.ExerciseRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Choisit l'exercice du jour (US-4.2) parmi ceux du catalogue.
 *
 * <p>Le choix est <em>figé pour la journée</em> : une fois tiré, l'identifiant retenu est
 * enregistré avec la date. Tant que la journée ne change pas, c'est ce même exercice qui est
 * renvoyé, et il survit à la fermeture de l'application.</p>
 *
 * <p>C'est ce qui le rend insensible aux modifications du catalogue : le tirage initial dépend
 * du nombre d'exercices et de leur ordre, donc sans cette mémoire, créer ou supprimer un
 * exercice en cours de journée changeait l'exercice mis en avant.</p>
 *
 * <p>Le tirage est relancé dans deux cas seulement : le jour a changé, ou l'exercice retenu
 * n'est plus au catalogue — il a été supprimé entre-temps.</p>
 */
public final class DailyExerciseProvider {

    private static final SimpleDateFormat DAY_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private static final String PREFS_NAME = "exergame_prefs";
    private static final String KEY_DAY = "daily_exercise_day";
    private static final String KEY_EXERCISE_ID = "daily_exercise_id";

    private final SharedPreferences preferences;

    public DailyExerciseProvider(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Clé de la journée en cours, au format {@code yyyy-MM-dd}. */
    public static String todayKey() {
        return DAY_KEY_FORMAT.format(new Date());
    }

    /**
     * L'exercice mis en avant aujourd'hui, ou {@code null} si le catalogue est vide.
     */
    @Nullable
    public Exercise ofTheDay() {
        List<Exercise> catalogue = ExerciseRepository.getAll();
        if (catalogue.isEmpty()) {
            return null;
        }

        String today = todayKey();
        if (today.equals(preferences.getString(KEY_DAY, null))) {
            Exercise retained = ExerciseRepository.findById(preferences.getString(KEY_EXERCISE_ID, null));
            if (retained != null) {
                return retained;
            }
            // L'exercice du jour a été supprimé du catalogue : on en retire un autre.
        }

        Exercise picked = pickForDay(catalogue, today);
        preferences.edit()
                .putString(KEY_DAY, today)
                .putString(KEY_EXERCISE_ID, picked.getId())
                .apply();
        return picked;
    }

    /**
     * Tirage initial pour une journée donnée.
     *
     * <p>Aléatoire mais reproductible : le générateur est initialisé avec la date. Ce tirage
     * dépend en revanche du contenu du catalogue au moment où il a lieu, d'où la mémorisation
     * faite par {@link #ofTheDay()}.</p>
     */
    @VisibleForTesting
    @Nullable
    public static Exercise pickForDay(List<Exercise> catalogue, String dayKey) {
        if (catalogue == null || catalogue.isEmpty()) {
            return null;
        }
        int index = new Random(dayKey.hashCode()).nextInt(catalogue.size());
        return catalogue.get(index);
    }
}
