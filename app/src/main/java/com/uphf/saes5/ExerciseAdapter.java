package com.uphf.saes5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.VH> {
    public interface OnItemClickListener { void onItemClick(Exercise exercise); }

    private final List<Exercise> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public ExerciseAdapter(OnItemClickListener listener) { this.listener = listener; }

    public void setItems(List<Exercise> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_exercise, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Exercise e = items.get(position);
        holder.name.setText(e.getName());
        holder.meta.setText(e.getDifficulty() + " • " + e.getEquipment());
        holder.stars.setText(renderStars(e.getStars()));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(e));
    }

    private String renderStars(int s) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<s;i++) sb.append("★");
        for (int i=s;i<5;i++) sb.append("☆");
        return sb.toString();
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, meta, stars;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.ex_name);
            meta = v.findViewById(R.id.ex_meta);
            stars = v.findViewById(R.id.ex_stars);
        }
    }
}