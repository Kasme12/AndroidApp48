package com.photos48.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.photos48.android.R;
import com.photos48.android.databinding.ActivityAlbumBinding;
import com.photos48.android.model.Album;
import com.photos48.android.model.Photo;
import com.photos48.android.persistence.PhotoRepository;
import com.photos48.android.ui.adapters.PhotoAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying photos in an album.
 * Features: Add photos, remove photos, view photos, move photos
 */
public class AlbumActivity extends AppCompatActivity {

    private ActivityAlbumBinding binding;
    private PhotoRepository repository;
    private Album album;
    private List<Photo> photos;
    private PhotoAdapter adapter;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlbumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get album ID from intent
        String albumId = getIntent().getStringExtra("ALBUM_ID");
        String albumName = getIntent().getStringExtra("ALBUM_NAME");
        
        if (albumId == null && albumName != null) {
            // Fallback: lookup by name if only name provided
            repository = PhotoRepository.getInstance(this);
            album = repository.getAlbum(albumName);
        } else if (albumId != null) {
            // Preferred: lookup by ID
            repository = PhotoRepository.getInstance(this);
            album = repository.getAlbumById(albumId);
        }
        
        if (album == null) {
            finish();
            return;
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(album.getName());
        }

        photos = new ArrayList<>();
        loadPhotos();

        // Setup RecyclerView with grid layout
        adapter = new PhotoAdapter(photos, this::onPhotoClick, this::onPhotoLongClick);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerView.setAdapter(adapter);

        // FAB for adding photos - uses SAF, no permissions needed
        binding.fabAddPhoto.setOnClickListener(v -> openImagePicker());

        // Setup image picker using Storage Access Framework
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            addPhotoToAlbum(imageUri);
                        }
                    }
                });
    }

    private void loadPhotos() {
        photos.clear();
        for (String photoId : album.getPhotoIds()) {
            Photo photo = repository.getPhoto(photoId);
            if (photo != null) {
                photos.add(photo);
            }
        }
    }

    /**
     * Open system image picker using Storage Access Framework.
     * SAF doesn't require runtime permissions on any API level.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void addPhotoToAlbum(Uri imageUri) {
        try {
            // Take persistent permission to access this URI
            getContentResolver().takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception e) {
            // Some URIs may not support persistent permissions
            // Continue anyway - we'll handle loading failures gracefully
        }

        String uriString = imageUri.toString();
        
        // Add photo to album via repository (creates unique ID)
        String photoId = repository.addPhotoToAlbum(album.getName(), uriString);
        if (photoId == null) {
            Toast.makeText(this, "Photo already in album", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Refresh photo list
        loadPhotos();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Photo added", Toast.LENGTH_SHORT).show();
    }

    private void onPhotoClick(Photo photo, int position) {
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putExtra("ALBUM_ID", album.getId());
        intent.putExtra("ALBUM_NAME", album.getName());
        intent.putExtra("PHOTO_POSITION", position);
        startActivity(intent);
    }

    private void onPhotoLongClick(Photo photo, int position) {
        List<String> otherAlbums = repository.getAlbumNamesExcept(album.getName());
        
        List<String> options = new ArrayList<>();
        options.add("Remove from album");
        if (!otherAlbums.isEmpty()) {
            options.add("Move to another album");
        }

        new AlertDialog.Builder(this)
                .setTitle("Photo Options")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        showRemovePhotoDialog(photo, position);
                    } else if (which == 1) {
                        showMovePhotoDialog(photo, position, otherAlbums);
                    }
                })
                .show();
    }

    private void showRemovePhotoDialog(Photo photo, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Photo")
                .setMessage("Remove this photo from the album?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    repository.removePhotoFromAlbum(album.getName(), photo.getId());
                    loadPhotos();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMovePhotoDialog(Photo photo, int position, List<String> otherAlbums) {
        new AlertDialog.Builder(this)
                .setTitle("Move Photo To")
                .setItems(otherAlbums.toArray(new String[0]), (dialog, which) -> {
                    String targetAlbumName = otherAlbums.get(which);
                    if (repository.movePhoto(photo.getId(), album.getName(), targetAlbumName)) {
                        loadPhotos();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Photo moved to " + targetAlbumName, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload photos in case tags were updated
        loadPhotos();
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
