package com.uphf.saes5.accueil.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.uphf.saes5.R;
import com.uphf.saes5.accueil.data.DailyExerciseTracker;
import com.uphf.saes5.accueil.data.ExerciseRepositoryProvider;
import com.uphf.saes5.databinding.ActivityExerciseBinding;
import com.uphf.saes5.accueil.model.Exercise;

/**
 * Écran d'exécution d'un exercice.
 *
 * <p>La validation des mouvements est pour l'instant <em>manuelle</em> : deux boutons remplacent
 * le moteur de reconnaissance (US-1.2 / US-1.3), qui n'est pas encore disponible. Toute la
 * mécanique autour (compteur, chronomètre, feedback, calcul des statistiques) est en revanche
 * définitive : brancher MediaPipe consistera à appeler {@link #onRepetitionValidated()} et
 * {@link #onRepetitionFailed()} depuis l'analyseur de pose au lieu des clics.</p>
 */
public class ExerciseActivity extends AppCompatActivity {

    private static final String EXTRA_EXERCISE_ID = "extra_exercise_id";
    private static final long FEEDBACK_DURATION_MILLIS = 1_200L;

    private ActivityExerciseBinding binding;
    private Exercise exercise;
    private DailyExerciseTracker dailyExerciseTracker;

    private int validatedReps;
    private int failedReps;
    private long startElapsedRealtime;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable chronometerTick = new Runnable() {
        @Override
        public void run() {
            updateChronometer();
            handler.postDelayed(this, 1_000L);
        }
    };
    private final Runnable hideFeedback = () -> binding.feedbackBanner.setVisibility(View.INVISIBLE);

    public static Intent newIntent(Context context, String exerciseId) {
        Intent intent = new Intent(context, ExerciseActivity.class);
        intent.putExtra(EXTRA_EXERCISE_ID, exerciseId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        exercise = ExerciseRepositoryProvider.get().findById(exerciseId);
        if (exercise == null) {
            // L'exercice n'existe plus (catalogue modifié) : on ne peut rien afficher.
            finish();
            return;
        }

        binding = ActivityExerciseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeSupport.applySystemBarInsets(binding.main);

        dailyExerciseTracker = new DailyExerciseTracker(this);
        startElapsedRealtime = SystemClock.elapsedRealtime();

        binding.exerciseName.setText(exercise.getName());
        updateRepCounter();
        updateChronometer();

        binding.validateRepButton.setOnClickListener(view -> onRepetitionValidated());
        binding.failRepButton.setOnClickListener(view -> onRepetitionFailed());
        binding.finishButton.setOnClickListener(view -> finishExercise());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmQuit();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(chronometerTick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(chronometerTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    /** Une répétition complète et correcte vient d'être détectée. */
    private void onRepetitionValidated() {
        validatedReps++;
        updateRepCounter();
        showFeedback(R.string.exercise_feedback_valid,
                R.color.app_success_container, R.color.app_on_success_container);
    }

    /** Un mouvement a été tenté mais n'est pas conforme à la référence : il n'est pas compté. */
    private void onRepetitionFailed() {
        failedReps++;
        showFeedback(R.string.exercise_feedback_failed,
                R.color.app_error_container, R.color.app_on_error_container);
    }

    private void updateRepCounter() {
        binding.repCounter.setText(
                getString(R.string.exercise_rep_counter, validatedReps, exercise.getTargetReps()));
    }

    private void updateChronometer() {
        binding.chronometer.setText(ExerciseFormatter.formatDuration(elapsedMillis()));
    }

    private long elapsedMillis() {
        return SystemClock.elapsedRealtime() - startElapsedRealtime;
    }

    private void showFeedback(@StringRes int messageRes,
                             @ColorRes int backgroundRes,
                             @ColorRes int textColorRes) {
        binding.feedbackBanner.setText(messageRes);
        binding.feedbackBanner.setBackgroundColor(ContextCompat.getColor(this, backgroundRes));
        binding.feedbackBanner.setTextColor(ContextCompat.getColor(this, textColorRes));
        binding.feedbackBanner.setVisibility(View.VISIBLE);

        handler.removeCallbacks(hideFeedback);
        handler.postDelayed(hideFeedback, FEEDBACK_DURATION_MILLIS);
    }

    private void confirmQuit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exercise_quit_title)
                .setMessage(R.string.exercise_quit_message)
                .setNegativeButton(R.string.exercise_quit_cancel, null)
                .setPositiveButton(R.string.exercise_quit_confirm, (dialog, which) -> finish())
                .show();
    }

    private void finishExercise() {
        long duration = elapsedMillis();

        // Une session sans aucune répétition validée n'est pas comptée comme réalisée (US-4.2).
        if (validatedReps > 0) {
            dailyExerciseTracker.markCompletedToday();
        }

        startActivity(SummaryActivity.newIntent(
                this, exercise.getId(), validatedReps, failedReps, duration));
        finish();
    }
}
