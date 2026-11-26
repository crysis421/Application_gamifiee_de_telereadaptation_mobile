package com.uphf.saes5.ui.catalog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.uphf.saes5.R;
import com.uphf.saes5.databinding.ActivityExercisePlayerBinding;
import com.uphf.saes5.di.AppContainer;
import com.uphf.saes5.di.ViewModelFactory;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.ui.common.Navigation;
import com.uphf.saes5.ui.exercise.ExerciseActivity;

/**
 * Fiche d'un exercice ouverte depuis le catalogue, avec le bouton qui lance la séance.
 *
 * <p>Elle porte la barre d'onglets, sur l'onglet Catalogue, sans être elle-même un onglet.</p>
 */
public class ExerciseDetailActivity extends AppCompatActivity {

    private static final String EXTRA_EXERCISE_ID = "exercise_id";

    public static Intent newIntent(Context context, String exerciseId) {
        return new Intent(context, ExerciseDetailActivity.class)
                .putExtra(EXTRA_EXERCISE_ID, exerciseId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityExercisePlayerBinding binding =
                ActivityExercisePlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String exerciseId = getIntent().getStringExtra(EXTRA_EXERCISE_ID);
        AppContainer container = AppContainer.from(this);
        ExerciseDetailViewModel viewModel = new ViewModelProvider(this, ViewModelFactory.of(
                ExerciseDetailViewModel.class,
                () -> new ExerciseDetailViewModel(
                        exerciseId, container.exerciseRepository, container.gameSettings)))
                .get(ExerciseDetailViewModel.class);

        Exercise exercise = viewModel.getExercise();
        if (exercise != null) {
            binding.playerName.setText(exercise.getName());
            binding.playerMetadata.setText(exercise.getMuscleGroup() + " • "
                    + exercise.getDifficulty() + " • " + exercise.getEquipment());
            binding.startButton.setOnClickListener(v ->
                    startActivity(ExerciseActivity.newIntent(this, exercise.getId())));
        }
        Navigation.setupTabs(this, findViewById(R.id.bottom_nav), R.id.nav_catalog);
        Navigation.setStatisticsVisible(findViewById(R.id.bottom_nav),
                viewModel.isStatisticsTabVisible());
    }
}
