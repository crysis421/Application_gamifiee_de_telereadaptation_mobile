package com.uphf.saes5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ProfileHistoryAdapter
        extends RecyclerView.Adapter<ProfileHistoryAdapter.HistoryViewHolder> {
    private final List<ProfileHistoryEntry> entries = new ArrayList<>();

    public void submitList(List<ProfileHistoryEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_history, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ProfileHistoryEntry entry = entries.get(position);
        holder.exercise.setText(entry.getExerciseName());
        holder.details.setText(holder.itemView.getContext().getString(
                R.string.profile_history_details,
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                        .format(new Date(entry.getCompletedAtEpochMillis())),
                entry.getMuscleGroup()));
        holder.score.setText(holder.itemView.getContext().getString(
                R.string.statistics_score, entry.getQualityScore()));
        holder.star.setVisibility(entry.earnedStar() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static final class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView exercise;
        private final TextView details;
        private final TextView score;
        private final TextView star;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            exercise = itemView.findViewById(R.id.profile_history_exercise);
            details = itemView.findViewById(R.id.profile_history_details);
            score = itemView.findViewById(R.id.profile_history_score);
            star = itemView.findViewById(R.id.profile_history_star);
        }
    }
}
