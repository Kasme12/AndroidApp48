package com.example.photosapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import java.util.ArrayList;
import android.content.Intent;


public class MainActivity extends AppCompatActivity {

    private ListView albumListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> albumNames;
    private int selectedAlbumIndex = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        albumListView = findViewById(R.id.albumListView);
        albumNames = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("albums", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("albumNames", new HashSet<>());

        if (set != null) {
            albumNames.addAll(set);
        }


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, albumNames);
        albumListView.setAdapter(adapter);

        Button createBtn = findViewById(R.id.createAlbumBtn);
        Button renameBtn = findViewById(R.id.renameAlbumBtn);
        Button deleteBtn = findViewById(R.id.deleteAlbumBtn);

        createBtn.setOnClickListener(v -> showCreateDialog());
        renameBtn.setOnClickListener(v -> showRenameDialog());
        deleteBtn.setOnClickListener(v -> showDeleteDialog());

        albumListView.setOnItemClickListener((parent, view, position, id) -> {
            if (selectedAlbumIndex == position) {
                // Open album on second tap
                String selectedAlbum = albumNames.get(position);
                Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                intent.putExtra("albumName", selectedAlbum);
                startActivity(intent);
            } else {
                // Just select it on first tap
                selectedAlbumIndex = position;
                albumListView.setItemChecked(position, true);
            }
        });

        Button searchBtn = findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });

    }

    private void showCreateDialog() {
        EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Create Album")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String name = input.getText().toString();
                    if (!name.isEmpty()) {
                        albumNames.add(name);
                        adapter.notifyDataSetChanged();

                        SharedPreferences prefs = getSharedPreferences("albums", MODE_PRIVATE);
                        Set<String> set = new HashSet<>(albumNames);
                        prefs.edit().putStringSet("albumNames", set).apply();

                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameDialog() {
        if (selectedAlbumIndex == -1) {
            Toast.makeText(this, "Please select an album to rename", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setText(albumNames.get(selectedAlbumIndex));
        new AlertDialog.Builder(this)
                .setTitle("Rename Album")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !albumNames.contains(newName)) {
                        String oldName = albumNames.get(selectedAlbumIndex);

                        albumNames.set(selectedAlbumIndex, newName);
                        adapter.notifyDataSetChanged();

                        SharedPreferences prefs = getSharedPreferences("albums", MODE_PRIVATE);
                        Set<String> set = new HashSet<>(albumNames);
                        prefs.edit().putStringSet("albumNames", set).apply();

                        SharedPreferences photoPrefs = getSharedPreferences("photos", MODE_PRIVATE);
                        Set<String> photos = getSharedPreferences("photos", MODE_PRIVATE)
                                .getStringSet(oldName, new HashSet<>());
                        getSharedPreferences("photos", MODE_PRIVATE)
                                .edit()
                                .remove(oldName)
                                .putStringSet(newName, photos)
                                .apply();

                        selectedAlbumIndex = -1;
                    } else {
                        Toast.makeText(this, "Invalid or duplicate album name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog() {
        if (selectedAlbumIndex == -1) {
            Toast.makeText(this, "Please select an album to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String albumToDelete = albumNames.get(selectedAlbumIndex);
        new AlertDialog.Builder(this)
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete " + albumToDelete + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Remove album name
                    albumNames.remove(selectedAlbumIndex);
                    adapter.notifyDataSetChanged();

                    SharedPreferences prefs = getSharedPreferences("albums", MODE_PRIVATE);
                    Set<String> set = new HashSet<>(albumNames);
                    prefs.edit().putStringSet("albumNames", set).apply();

                    SharedPreferences photoPrefs = getSharedPreferences("photos", MODE_PRIVATE);
                    Set<String> deletedPhotoUris = photoPrefs.getStringSet(albumToDelete, new HashSet<>());

                    // Remove the album
                    photoPrefs.edit().remove(albumToDelete).apply();

                    // Check if deleted photos still exist in any other album
                    SharedPreferences allAlbumsPrefs = getSharedPreferences("photos", MODE_PRIVATE);
                    Map<String, ?> allAlbums = allAlbumsPrefs.getAll();
                    SharedPreferences tagPrefs = getSharedPreferences("tags", MODE_PRIVATE);

                    for (String uri : deletedPhotoUris) {
                        boolean stillExists = false;
                        for (Object value : allAlbums.values()) {
                            if (value instanceof Set && ((Set<?>) value).contains(uri)) {
                                stillExists = true;
                                break;
                            }
                        }
                        if (!stillExists) {
                            tagPrefs.edit().remove(uri).apply();
                        }
                    }

                    selectedAlbumIndex = -1;
                })
                .setNegativeButton("No", null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        selectedAlbumIndex = -1;
        albumListView.clearChoices();
        adapter.notifyDataSetChanged();
    }

}
