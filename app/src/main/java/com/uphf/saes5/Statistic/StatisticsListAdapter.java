package com.uphf.saes5;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class StatisticsListAdapter
        extends RecyclerView.Adapter<StatisticsListAdapter.StatisticViewHolder> {
    private final List<StatisticListItem> items = new ArrayList<>();

    public void submitList(List<StatisticListItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatisticViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_statistic, parent, false);
        return new StatisticViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatisticViewHolder holder, int position) {
        StatisticListItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.subtitle.setVisibility(item.getSubtitle() == null ? View.GONE : View.VISIBLE);
        holder.subtitle.setText(item.getSubtitle());

        if (item.hasData()) {
            holder.score.setText(holder.itemView.getContext().getString(
                    R.string.statistics_score, item.getScore()));
            holder.progress.setProgressCompat(item.getScore(), false);
        } else {
            holder.score.setText(R.string.statistics_no_data);
            holder.progress.setProgressCompat(0, false);
        }
        holder.progress.setIndicatorColor(progressColor(holder, item));

        String sessions = holder.itemView.getResources().getQuantityString(
                R.plurals.statistics_sessions, item.getSessionCount(), item.getSessionCount());
        if (item.getPerfectPercentage() == null) {
            holder.details.setText(sessions);
        } else {
            holder.details.setText(holder.itemView.getContext().getString(
                    R.string.statistics_exercise_details,
                    sessions, item.getPerfectPercentage()));
        }
    }

    private int progressColor(StatisticViewHolder holder, StatisticListItem item) {
        int color;
        if (!item.hasData()) color = R.color.muscle_no_data;
        else if (item.getScore() < 40) color = R.color.muscle_progress_low;
        else if (item.getScore() < 70) color = R.color.muscle_progress_medium;
        else color = R.color.muscle_progress_high;
        return ContextCompat.getColor(holder.itemView.getContext(), color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StatisticViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView score;
        final TextView details;
        final LinearProgressIndicator progress;

        StatisticViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.statistic_title);
            subtitle = itemView.findViewById(R.id.statistic_subtitle);
            score = itemView.findViewById(R.id.statistic_score);
            details = itemView.findViewById(R.id.statistic_details);
            progress = itemView.findViewById(R.id.statistic_progress);
        }
    }
}
