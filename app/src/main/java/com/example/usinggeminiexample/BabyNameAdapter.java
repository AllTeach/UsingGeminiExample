package com.example.usinggeminiexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BabyNameAdapter extends RecyclerView.Adapter<BabyNameAdapter.BabyNameViewHolder> {

    private final List<BabyName> babyNameList;

    public BabyNameAdapter(List<BabyName> babyNameList) {
        this.babyNameList = babyNameList;
    }

    @NonNull
    @Override
    public BabyNameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item, parent, false);
        return new BabyNameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BabyNameViewHolder holder, int position) {
        BabyName babyName = babyNameList.get(position);

        holder.babyNameTextView.setText(babyName.getName());
        holder.meaningTextView.setText("Meaning: " + babyName.getMeaning());
        holder.originTextView.setText("Origin: " + babyName.getOrigin());
        holder.detailsTextView.setText("Details: " + babyName.getDetails());

        boolean isExpanded = babyName.isExpanded();
        holder.detailsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        holder.cardView.setOnClickListener(v -> {
            babyName.setExpanded(!isExpanded);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return babyNameList.size();
    }

    public static class BabyNameViewHolder extends RecyclerView.ViewHolder {
        private final TextView babyNameTextView;
        private final LinearLayout detailsLayout;
        private final TextView meaningTextView;
        private final TextView originTextView;
        private final TextView detailsTextView;
        private final CardView cardView;

        public BabyNameViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.babyNameCardView);
            babyNameTextView = itemView.findViewById(R.id.babyNameTextView);
            detailsLayout = itemView.findViewById(R.id.detailsLayout);
            meaningTextView = itemView.findViewById(R.id.meaningTextView);
            originTextView = itemView.findViewById(R.id.originTextView);
            detailsTextView = itemView.findViewById(R.id.detailsTextView);
        }
    }
}