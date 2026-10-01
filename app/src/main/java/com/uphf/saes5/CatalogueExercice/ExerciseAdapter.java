package com.uphf.saes5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    public interface OnExerciseClickListener { void onClick(Exercise exercise); }

    /**
     * Ouverture du menu « modifier / supprimer ».
     *
     * <p>L'adaptateur ne construit pas le menu lui-même : il signale le clic et passe la vue
     * qui doit lui servir d'ancre, pour que l'écran décide quoi proposer.</p>
     */
    public interface OnExerciseMenuListener { void onMenuClick(Exercise exercise, View anchor); }

    private final List<Exercise> exercises = new ArrayList<>();
    private final OnExerciseClickListener listener;
    private final OnExerciseMenuListener menuListener;

    public ExerciseAdapter(OnExerciseClickListener listener, OnExerciseMenuListener menuListener) {
        this.listener = listener;
        this.menuListener = menuListener;
    }

    public void submitList(List<Exercise> newExercises) {
        exercises.clear();
        if (newExercises != null) exercises.addAll(newExercises);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.muscle.setText(exercise.getMuscleGroup());
        holder.name.setText(exercise.getName());
        holder.metadata.setText(exercise.getDifficulty() + " • " + exercise.getEquipment()
                + " • " + exercise.getDurationMinutes() + " min");
        holder.stars.setText(stars(exercise.getStars()));
        holder.itemView.setOnClickListener(view -> listener.onClick(exercise));
        holder.menuButton.setOnClickListener(view -> menuListener.onMenuClick(exercise, view));
        holder.menuButton.setContentDescription(
                holder.itemView.getContext().getString(R.string.exercise_options, exercise.getName()));
    }

    private String stars(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 5; i++) result.append(i < count ? "★" : "☆");
        return result.toString();
    }

    @Override
    public int getItemCount() { return exercises.size(); }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        final TextView muscle;
        final TextView name;
        final TextView metadata;
        final TextView stars;
        final ImageButton menuButton;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            muscle = itemView.findViewById(R.id.exercise_muscle);
            name = itemView.findViewById(R.id.exercise_name);
            metadata = itemView.findViewById(R.id.exercise_metadata);
            stars = itemView.findViewById(R.id.exercise_stars);
            menuButton = itemView.findViewById(R.id.exercise_menu_button);
        }
    }
}
