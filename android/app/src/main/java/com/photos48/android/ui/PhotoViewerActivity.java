package com.photos48.android.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.photos48.android.R;
import com.photos48.android.databinding.ActivityPhotoViewerBinding;
import com.photos48.android.model.Album;
import com.photos48.android.model.Photo;
import com.photos48.android.model.Tag;
import com.photos48.android.persistence.PhotoRepository;
import com.photos48.android.ui.adapters.TagAdapter;
import com.photos48.android.utils.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for viewing a single photo with slideshow controls.
 * Features: View photo, add/delete tags, navigate prev/next
 * State preservation: Current position saved across configuration changes
 */
public class PhotoViewerActivity extends AppCompatActivity {

    private static final String KEY_CURRENT_POSITION = "current_position";
    
    private ActivityPhotoViewerBinding binding;
    private PhotoRepository repository;
    private Album album;
    private List<Photo> photos;
    private int currentPosition;
    private TagAdapter tagAdapter;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize threading components
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Get album from intent (support both ID and name)
        String albumId = getIntent().getStringExtra("ALBUM_ID");
        String albumName = getIntent().getStringExtra("ALBUM_NAME");
        
        // Restore position from saved state, otherwise use intent
        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
        } else {
            currentPosition = getIntent().getIntExtra("PHOTO_POSITION", 0);
        }

        repository = PhotoRepository.getInstance(this);
        
        if (albumId != null) {
            album = repository.getAlbumById(albumId);
        } else if (albumName != null) {
            album = repository.getAlbum(albumName);
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

        // Load photos
        photos = new ArrayList<>();
        for (String photoId : album.getPhotoIds()) {
            Photo photo = repository.getPhoto(photoId);
            if (photo != null) {
                photos.add(photo);
            }
        }

        if (photos.isEmpty() || currentPosition >= photos.size()) {
            finish();
            return;
        }

        // Setup tag list
        tagAdapter = new TagAdapter(new ArrayList<>(), this::onTagClick);
        binding.tagRecyclerView.setAdapter(tagAdapter);

        // Navigation buttons
        binding.btnPrevious.setOnClickListener(v -> navigatePrevious());
        binding.btnNext.setOnClickListener(v -> navigateNext());
        binding.fabAddTag.setOnClickListener(v -> showAddTagDialog());
        binding.btnMovePhoto.setOnClickListener(v -> showMovePhotoDialog());

        displayPhoto();
    }

    private void displayPhoto() {
        if (currentPosition < 0 || currentPosition >= photos.size()) {
            return;
        }

        Photo photo = photos.get(currentPosition);
        
        // Update position indicator immediately
        binding.tvPosition.setText(String.format("%d / %d", currentPosition + 1, photos.size()));
        
        // Update filename display
        String filename = photo.getFilename();
        binding.tvFilename.setText(filename);

        // Update navigation buttons (edge case handling)
        binding.btnPrevious.setEnabled(currentPosition > 0);
        binding.btnNext.setEnabled(currentPosition < photos.size() - 1);

        // Update tags
        tagAdapter.updateTags(new ArrayList<>(photo.getTags()));
        
        // Load image in background to keep UI responsive
        loadImageAsync(photo.getUri());
    }
    
    /**
     * Load full-size image in background thread to prevent UI blocking.
     * Uses ExecutorService for background work and Handler for UI updates.
     */
    private void loadImageAsync(String uri) {
        // Show placeholder immediately
        binding.imageView.setImageResource(R.drawable.ic_image_placeholder);
        binding.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        
        executorService.execute(() -> {
            // Load bitmap in background (0 = no downsampling for full viewer)
            final Bitmap bitmap = ImageLoader.loadThumbnail(this, uri, 0);
            
            // Update UI on main thread
            mainHandler.post(() -> {
                if (bitmap != null) {
                    binding.imageView.setImageBitmap(bitmap);
                    binding.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                } else {
                    // Keep placeholder if load failed
                    binding.imageView.setImageResource(R.drawable.ic_image_placeholder);
                    binding.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
            });
        });
    }

    private void navigatePrevious() {
        if (currentPosition > 0) {
            currentPosition--;
            displayPhoto();
        }
    }

    private void navigateNext() {
        if (currentPosition < photos.size() - 1) {
            currentPosition++;
            displayPhoto();
        }
    }

    private void showAddTagDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tag, null);
        
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_tag_type);
        AutoCompleteTextView actvValue = dialogView.findViewById(R.id.actv_tag_value);

        // Tag types: only Person and Location (with capital first letter for display)
        String[] tagTypes = {"Person", "Location"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_dropdown_item, tagTypes);
        spinnerType.setAdapter(typeAdapter);

        // Setup value autocomplete based on selected type
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = tagTypes[position].toLowerCase();
                List<String> existingValues = repository.getAllTagValues(selectedType);
                ArrayAdapter<String> valueAdapter = new ArrayAdapter<>(
                    PhotoViewerActivity.this,
                    android.R.layout.simple_dropdown_item_1line, 
                    existingValues);
                actvValue.setAdapter(valueAdapter);
                actvValue.setThreshold(1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default to empty adapter
                actvValue.setAdapter(null);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Tag")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String type = tagTypes[spinnerType.getSelectedItemPosition()].toLowerCase();
                    String value = actvValue.getText().toString().trim();

                    // Validation: disallow empty value
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Tag value cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Photo photo = photos.get(currentPosition);
                    Tag newTag;
                    try {
                        // Create tag with trimmed value (preserves original case)
                        newTag = new Tag(type, value);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(this, "Invalid tag type", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // addTagToPhoto checks for case-insensitive duplicates
                    if (!repository.addTagToPhoto(photo.getId(), newTag)) {
                        Toast.makeText(this, "Tag already exists (case-insensitive match)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Immediately reflect changes in UI
                    tagAdapter.updateTags(photo.getTags());
                    Toast.makeText(this, "Tag added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onTagClick(Tag tag) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Tag")
                .setMessage("Delete tag: " + tag.getType() + " = " + tag.getValue() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Photo photo = photos.get(currentPosition);
                    repository.removeTagFromPhoto(photo.getId(), tag);
                    
                    // Immediately reflect changes in UI
                    tagAdapter.updateTags(photo.getTags());
                    Toast.makeText(this, "Tag deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show dialog to move photo to another album.
     * Lists all other albums (excluding current album).
     */
    private void showMovePhotoDialog() {
        List<Album> allAlbums = repository.getAlbums();
        
        // Filter out current album to prevent moving to same album
        List<Album> otherAlbums = new ArrayList<>();
        for (Album a : allAlbums) {
            if (!a.getId().equals(album.getId())) {
                otherAlbums.add(a);
            }
        }
        
        if (otherAlbums.isEmpty()) {
            Toast.makeText(this, "No other albums available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create array of album names for display
        String[] albumNames = new String[otherAlbums.size()];
        for (int i = 0; i < otherAlbums.size(); i++) {
            albumNames[i] = otherAlbums.get(i).getName();
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Move Photo")
                .setItems(albumNames, (dialog, which) -> {
                    Album targetAlbum = otherAlbums.get(which);
                    Photo currentPhoto = photos.get(currentPosition);
                    
                    // Move photo entry (keeps all metadata and tags with the moved instance)
                    if (repository.movePhotoById(currentPhoto.getId(), album.getId(), targetAlbum.getId())) {
                        Toast.makeText(this, "Photo moved to " + targetAlbum.getName(), Toast.LENGTH_SHORT).show();
                        
                        // Photo no longer in current album, close viewer and return to album
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to move photo", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current position across configuration changes (rotation)
        outState.putInt(KEY_CURRENT_POSITION, currentPosition);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up executor service to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
