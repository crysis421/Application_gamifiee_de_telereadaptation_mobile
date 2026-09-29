package com.uphf.saes5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    public interface OnExerciseClickListener { void onClick(Exercise exercise); }

    private final List<Exercise> exercises = new ArrayList<>();
    private final OnExerciseClickListener listener;

    public ExerciseAdapter(OnExerciseClickListener listener) {
        this.listener = listener;
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
        holder.name.setText(exercise.getName());
        holder.metadata.setText(exercise.getDifficulty() + " • " + exercise.getEquipment()
                + " • " + exercise.getDurationMinutes() + " min");
        holder.stars.setText(stars(exercise.getStars()));
        holder.itemView.setOnClickListener(view -> listener.onClick(exercise));
    }

    private String stars(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 5; i++) result.append(i < count ? "★" : "☆");
        return result.toString();
    }

    @Override
    public int getItemCount() { return exercises.size(); }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView metadata;
        final TextView stars;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.exercise_name);
            metadata = itemView.findViewById(R.id.exercise_metadata);
            stars = itemView.findViewById(R.id.exercise_stars);
        }
    }
}
