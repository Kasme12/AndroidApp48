package com.photos48.android.ui.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.photos48.android.R;
import com.photos48.android.model.Photo;
import com.photos48.android.util.ImageLoader;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private final List<Photo> photos;
    private final OnPhotoClickListener clickListener;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
    }

    public SearchResultAdapter(List<Photo> photos, OnPhotoClickListener clickListener) {
        this.photos = photos;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.bind(photo);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }

        void bind(Photo photo) {
            // Load thumbnail efficiently
            Bitmap thumbnail = ImageLoader.loadThumbnail(itemView.getContext(), photo.getUri());
            
            if (thumbnail != null) {
                imageView.setImageBitmap(thumbnail);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView.setImageResource(R.drawable.ic_image_placeholder);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
            
            itemView.setOnClickListener(v -> clickListener.onPhotoClick(photo));
        }
    }
}
