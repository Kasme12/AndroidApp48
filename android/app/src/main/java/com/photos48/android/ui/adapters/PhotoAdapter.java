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

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    private final List<Photo> photos;
    private final OnPhotoClickListener clickListener;
    private final OnPhotoLongClickListener longClickListener;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo, int position);
    }

    public interface OnPhotoLongClickListener {
        void onPhotoLongClick(Photo photo, int position);
    }

    public PhotoAdapter(List<Photo> photos, OnPhotoClickListener clickListener,
                       OnPhotoLongClickListener longClickListener) {
        this.photos = photos;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
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
        holder.bind(photo, position);
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

        void bind(Photo photo, int position) {
            // Load thumbnail efficiently using ImageLoader
            Bitmap thumbnail = ImageLoader.loadThumbnail(itemView.getContext(), photo.getUri());
            
            if (thumbnail != null) {
                imageView.setImageBitmap(thumbnail);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                // Fallback to placeholder if loading fails
                imageView.setImageResource(R.drawable.ic_image_placeholder);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            itemView.setOnClickListener(v -> clickListener.onPhotoClick(photo, position));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onPhotoLongClick(photo, position);
                return true;
            });
        }
    }
}
