package com.photos48.android.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.photos48.android.R;
import com.photos48.android.model.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying tags as Material Design chips with delete functionality.
 */
public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {

    private List<Tag> tags;
    private final OnTagClickListener clickListener;

    public interface OnTagClickListener {
        void onTagClick(Tag tag);
    }

    public TagAdapter(List<Tag> tags, OnTagClickListener clickListener) {
        this.tags = tags;
        this.clickListener = clickListener;
    }

    public void updateTags(List<Tag> newTags) {
        this.tags = newTags != null ? newTags : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tag tag = tags.get(position);
        holder.bind(tag);
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final Chip chipTag;

        ViewHolder(View itemView) {
            super(itemView);
            chipTag = itemView.findViewById(R.id.chip_tag);
        }

        void bind(Tag tag) {
            // Format: "person: John Doe" or "location: New York"
            String chipText = String.format("%s: %s", 
                tag.getType().substring(0, 1).toUpperCase() + tag.getType().substring(1),
                tag.getValue());
            chipTag.setText(chipText);
            
            // Delete on close icon click
            chipTag.setOnCloseIconClickListener(v -> clickListener.onTagClick(tag));
        }
    }
}
