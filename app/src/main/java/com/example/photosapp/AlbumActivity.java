package com.example.photosapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import java.util.HashSet;
import java.util.Set;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.Map;

import java.util.ArrayList;

import android.view.View;
import android.view.ViewGroup;



public class AlbumActivity extends AppCompatActivity {

    private GridView photoGrid;
    private ArrayList<String> photoUris;

    private PhotoAdapter photoAdapter;
    private String albumName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        albumName = getIntent().getStringExtra("albumName");

        // Load saved photo URIs from SharedPreferences
        Set<String> savedUris = getSharedPreferences("photos", MODE_PRIVATE).getStringSet(albumName, null);
        photoUris = new ArrayList<>();
        if (savedUris != null) {
            photoUris.addAll(savedUris);
        }

        TextView title = findViewById(R.id.albumTitle);
        title.setText("Album: " + albumName);

        photoGrid = findViewById(R.id.photoGrid);
        photoAdapter = new PhotoAdapter();
        photoGrid.setAdapter(photoAdapter);

        Button addPhotoButton = findViewById(R.id.addPhotoButton);
        addPhotoButton.setOnClickListener(v -> pickPhoto.launch(new String[] {"image/*"}));
    }


    // Register the activity result launcher
    private final ActivityResultLauncher<String[]> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    String uriString = uri.toString();

                    if (photoUris.contains(uriString)) {
                        Toast.makeText(this, "Photo already exists in this album", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    photoUris.add(uriString);
                    savePhotoUris();
                    photoAdapter.notifyDataSetChanged();
                }
            });


    private void savePhotoUris() {
        getSharedPreferences("photos", MODE_PRIVATE)
                .edit()
                .putStringSet(albumName, new HashSet<>(photoUris))
                .apply();
    }


    // Adapter to show image thumbnails
    private class PhotoAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return photoUris.size();
        }

        @Override
        public Object getItem(int position) {
            return photoUris.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(AlbumActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(8, 8, 8, 8);

            ImageView imageView = new ImageView(AlbumActivity.this);
            imageView.setImageURI(Uri.parse(photoUris.get(position)));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new GridView.LayoutParams(300, 300));
            layout.addView(imageView);

            // Create "Move" button
            Button moveButton = new Button(AlbumActivity.this);
            moveButton.setText("Move");
            layout.addView(moveButton);

            // Create "Delete" button
            Button deleteButton = new Button(AlbumActivity.this);
            deleteButton.setText("Delete");
            layout.addView(deleteButton);

            // Fullscreen image view
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(AlbumActivity.this, PhotoViewActivity.class);
                intent.putStringArrayListExtra("photoUris", photoUris);
                intent.putExtra("position", position);
                startActivity(intent);
            });

            // Move photo action
            moveButton.setOnClickListener(v -> showMoveDialog(position));

            // Delete photo action
            deleteButton.setOnClickListener(v -> {
                String uri = photoUris.get(position);

                SharedPreferences prefs = getSharedPreferences("photos", MODE_PRIVATE);
                Set<String> set = new HashSet<>(prefs.getStringSet(albumName, new HashSet<>()));
                set.remove(uri);
                prefs.edit().putStringSet(albumName, set).apply();

                photoUris.remove(position);
                notifyDataSetChanged();

                // Check if photo exists in any other albums
                boolean isInOtherAlbums = false;
                SharedPreferences allAlbumsPrefs = getSharedPreferences("photos", MODE_PRIVATE);
                Map<String, ?> allAlbums = allAlbumsPrefs.getAll();

                for (Object value : allAlbums.values()) {
                    if (value instanceof Set) {
                        Set<String> photoUrisSet = (Set<String>) value;
                        if (photoUrisSet.contains(uri)) {
                            isInOtherAlbums = true;
                            break;
                        }
                    }
                }

                if (!isInOtherAlbums) {
                    // Remove tag only if photo is no longer in any album
                    SharedPreferences tagPrefs = getSharedPreferences("tags", MODE_PRIVATE);
                    tagPrefs.edit().remove(uri).apply();
                }

                Toast.makeText(AlbumActivity.this, "Photo deleted", Toast.LENGTH_SHORT).show();
            });

            return layout;
        }




        private void showMoveDialog(int photoIndex) {
            // Get all albums from SharedPreferences
            Set<String> albumNamesSet = getSharedPreferences("albums", MODE_PRIVATE).getStringSet("albumNames", new HashSet<>());
            ArrayList<String> albumList = new ArrayList<>(albumNamesSet);

            // Remove the current album from the list
            albumList.remove(albumName);

            if (albumList.isEmpty()) {
                Toast.makeText(AlbumActivity.this, "No other albums to move to...", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] albumArray = albumList.toArray(new String[0]);

            new AlertDialog.Builder(AlbumActivity.this)

                    .setTitle("Move to album")
                    .setItems(albumArray, (dialog, which) -> {
                        String targetAlbum = albumArray[which];
                        String uri = photoUris.get(photoIndex);

                        SharedPreferences prefs = getSharedPreferences("photos", MODE_PRIVATE);

                        // Remove from current album
                        Set<String> currentSet = new HashSet<>(prefs.getStringSet(albumName, new HashSet<>()));
                        currentSet.remove(uri);
                        prefs.edit().putStringSet(albumName, currentSet).apply();

                        // Add to target album
                        Set<String> targetSet = new HashSet<>(prefs.getStringSet(targetAlbum, new HashSet<>()));
                        targetSet.add(uri);
                        prefs.edit().putStringSet(targetAlbum, targetSet).apply();

                        photoUris.remove(photoIndex);
                        photoAdapter.notifyDataSetChanged();

                        Toast.makeText(AlbumActivity.this, "Photo moved to " + targetAlbum, Toast.LENGTH_SHORT).show();

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

    }
}
