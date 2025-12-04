package com.photos48.android.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.photos48.android.R;
import com.photos48.android.model.Album;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    private final List<Album> albums;
    private final OnAlbumClickListener clickListener;
    private final OnAlbumLongClickListener longClickListener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album, int position);
    }

    public interface OnAlbumLongClickListener {
        void onAlbumLongClick(Album album, int position);
    }

    public AlbumAdapter(List<Album> albums, OnAlbumClickListener clickListener, 
                       OnAlbumLongClickListener longClickListener) {
        this.albums = albums;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    /**
     * Update the album list and refresh the adapter.
     */
    public void updateAlbums(List<Album> newAlbums) {
        this.albums = newAlbums;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.bind(album, position);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAlbumName;
        private final TextView tvPhotoCount;

        ViewHolder(View itemView) {
            super(itemView);
            tvAlbumName = itemView.findViewById(R.id.tv_album_name);
            tvPhotoCount = itemView.findViewById(R.id.tv_photo_count);
        }

        void bind(Album album, int position) {
            tvAlbumName.setText(album.getName());
            int count = album.getPhotoCount();
            tvPhotoCount.setText(String.format("%d photo%s", count, count == 1 ? "" : "s"));

            itemView.setOnClickListener(v -> clickListener.onAlbumClick(album, position));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onAlbumLongClick(album, position);
                return true;
            });
        }
    }
}
