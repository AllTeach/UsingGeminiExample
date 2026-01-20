package com.example.usinggeminiexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BabyNameAdapter extends RecyclerView.Adapter<BabyNameAdapter.NameViewHolder> {

    private final List<BabyName> babyNames;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(BabyName babyName, int position);
    }

    public BabyNameAdapter(List<BabyName> babyNames, OnItemClickListener listener) {
        this.babyNames = babyNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_baby_name, parent, false);
        return new NameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NameViewHolder holder, int position) {
        BabyName babyName = babyNames.get(position);

        holder.textViewName.setText(babyName.getName());
        holder.textViewOrigin.setText("מקור: " + babyName.getOrigin());
        holder.textViewMeaning.setText("משמעות: " + babyName.getMeaning());
        holder.textViewDetails.setText(babyName.getDetails());
        holder.textViewAlternatives.setText("אולי תאהבו גם: " + babyName.getAlternatives());

        // Handle Loading State
        if (babyName.isLoading()) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.layoutDetails.setVisibility(View.GONE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            // Handle Expanded State
            holder.layoutDetails.setVisibility(babyName.isExpanded() ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(babyName, position));
    }

    @Override
    public int getItemCount() {
        return babyNames.size();
    }

    static class NameViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewOrigin, textViewMeaning, textViewDetails, textViewAlternatives;
        LinearLayout layoutDetails;
        ProgressBar progressBar;

        public NameViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewOrigin = itemView.findViewById(R.id.textViewOrigin);
            textViewMeaning = itemView.findViewById(R.id.textViewMeaning);
            textViewDetails = itemView.findViewById(R.id.textViewDetails);
            textViewAlternatives = itemView.findViewById(R.id.textViewAlternatives);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
            progressBar = itemView.findViewById(R.id.progressBarLoading);
        }
    }
}