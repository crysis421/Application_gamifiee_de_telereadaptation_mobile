package com.uphf.saes5.ui.statistics;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.session.StatisticsRepository;
import com.uphf.saes5.data.settings.GameSettingsRepository;
import com.uphf.saes5.domain.model.GameFeature;
import com.uphf.saes5.domain.statistics.ExerciseStatistic;
import com.uphf.saes5.domain.statistics.MuscleStatistic;
import com.uphf.saes5.domain.statistics.ScoredSession;
import com.uphf.saes5.domain.statistics.StatisticsCalculator;
import com.uphf.saes5.domain.statistics.StatisticsExercise;
import com.uphf.saes5.domain.statistics.StatisticsSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Statistiques (US-5.3) : score global, carte musculaire et liste par muscle ou par exercice.
 *
 * <p>La sélection d'un muscle sur le corps filtre la liste ; la toucher de nouveau la retire.
 * Les lignes affichées sont préparées ici : l'écran n'a plus qu'à les donner à l'adapter.</p>
 */
public final class StatisticsViewModel extends ViewModel {

    private final Supplier<List<StatisticsExercise>> exercises;
    private final Supplier<List<ScoredSession>> sessions;
    private final BooleanSupplier statisticsTabVisible;
    private final MutableLiveData<StatisticsScreenState> state = new MutableLiveData<>();

    private StatisticsSummary summary;
    private StatisticsScreenState.DisplayMode displayMode = StatisticsScreenState.DisplayMode.MUSCLES;
    private String selectedMuscle;

    public StatisticsViewModel(StatisticsRepository repository,
                               GameSettingsRepository gameSettings) {
        this(repository::getExercises, repository::getSessions,
                () -> gameSettings.isEnabled(GameFeature.STATISTICS));
    }

    /** Données fournies directement : sert aux tests, qui n'ont pas de journal sur disque. */
    @VisibleForTesting
    StatisticsViewModel(Supplier<List<StatisticsExercise>> exercises,
                        Supplier<List<ScoredSession>> sessions) {
        this(exercises, sessions, () -> true);
    }

    private StatisticsViewModel(Supplier<List<StatisticsExercise>> exercises,
                                Supplier<List<ScoredSession>> sessions,
                                BooleanSupplier statisticsTabVisible) {
        this.exercises = exercises;
        this.sessions = sessions;
        this.statisticsTabVisible = statisticsTabVisible;
        refresh();
    }

    public LiveData<StatisticsScreenState> getState() {
        return state;
    }

    /**
     * Relit le journal : l'onglet est réutilisé (singleTop), et une séance a pu se terminer
     * depuis l'ouverture de l'écran. La sélection est conservée.
     */
    public void refresh() {
        summary = StatisticsCalculator.calculate(exercises.get(), sessions.get());
        publishState();
    }

    /** Sélectionne un muscle, ou retire la sélection s'il l'était déjà. */
    public void selectMuscle(String muscleGroup) {
        selectedMuscle = muscleGroup != null && muscleGroup.equals(selectedMuscle)
                ? null
                : muscleGroup;
        publishState();
    }

    public void showMuscleMode() {
        displayMode = StatisticsScreenState.DisplayMode.MUSCLES;
        publishState();
    }

    public void showExerciseMode() {
        displayMode = StatisticsScreenState.DisplayMode.EXERCISES;
        publishState();
    }

    private void publishState() {
        List<ExerciseStatistic> visibleExercises = new ArrayList<>();
        for (ExerciseStatistic statistic : summary.getExerciseStatistics()) {
            if (selectedMuscle == null || statistic.getMuscleGroup().equals(selectedMuscle)) {
                visibleExercises.add(statistic);
            }
        }

        boolean muscles = displayMode == StatisticsScreenState.DisplayMode.MUSCLES;
        StatisticsScreenState.Heading heading;
        if (muscles) {
            heading = selectedMuscle == null
                    ? StatisticsScreenState.Heading.MUSCLES_ALL
                    : StatisticsScreenState.Heading.MUSCLE_SELECTED;
        } else {
            heading = selectedMuscle == null
                    ? StatisticsScreenState.Heading.EXERCISES_ALL
                    : StatisticsScreenState.Heading.EXERCISES_FILTERED;
        }
        List<StatisticListItem> items = muscles
                ? StatisticsListMapper.fromMuscles(visibleMuscles())
                : StatisticsListMapper.fromExercises(visibleExercises);

        state.setValue(new StatisticsScreenState(summary, displayMode, selectedMuscle,
                visibleExercises, items, heading, statisticsTabVisible.getAsBoolean()));
    }

    private List<MuscleStatistic> visibleMuscles() {
        if (selectedMuscle == null) return summary.getMuscleStatistics();
        List<MuscleStatistic> result = new ArrayList<>();
        for (MuscleStatistic statistic : summary.getMuscleStatistics()) {
            if (selectedMuscle.equals(statistic.getMuscleGroup())) result.add(statistic);
        }
        return result;
    }
}
