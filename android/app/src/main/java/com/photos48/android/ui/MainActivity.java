package com.photos48.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputEditText;
import com.photos48.android.R;
import com.photos48.android.databinding.ActivityMainBinding;
import com.photos48.android.databinding.DialogAlbumNameBinding;
import com.photos48.android.model.Album;
import com.photos48.android.persistence.PhotoRepository;
import com.photos48.android.ui.adapters.AlbumAdapter;

import java.util.List;

/**
 * Main activity showing the home screen with list of albums.
 * Features: Create, delete, rename, open albums
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PhotoRepository repository;
    private AlbumAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Initialize repository and load data
        repository = PhotoRepository.getInstance(this);

        // Setup RecyclerView
        adapter = new AlbumAdapter(repository.getAlbums(), this::onAlbumClick, this::onAlbumLongClick);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        // FAB for creating new album
        binding.fabAddAlbum.setOnClickListener(v -> showCreateAlbumDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (id == R.id.action_load_test_data) {
            showLoadTestDataDialog();
            return true;
        } else if (id == R.id.action_clear_data) {
            showClearDataDialog();
            return true;
        } else if (id == R.id.action_show_stats) {
            showDataStats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onAlbumClick(Album album, int position) {
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra("ALBUM_ID", album.getId());
        intent.putExtra("ALBUM_NAME", album.getName());
        startActivity(intent);
    }

    private void onAlbumLongClick(Album album, int position) {
        new AlertDialog.Builder(this)
                .setTitle(album.getName())
                .setItems(new String[]{"Rename", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showRenameAlbumDialog(album, position);
                    } else {
                        showDeleteAlbumDialog(album, position);
                    }
                })
                .show();
    }

    private void showCreateAlbumDialog() {
        DialogAlbumNameBinding dialogBinding = DialogAlbumNameBinding.inflate(getLayoutInflater());
        TextInputEditText editText = dialogBinding.etAlbumName;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create Album")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Create", null) // Set to null, will override
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editText.getText() != null ? editText.getText().toString().trim() : "";
                
                // Validation: non-empty
                if (name.isEmpty()) {
                    editText.setError("Album name cannot be empty");
                    return;
                }
                
                // Validation: unique (case-insensitive)
                if (!repository.addAlbum(name)) {
                    editText.setError("Album already exists");
                    return;
                }
                
                // Success: refresh and dismiss
                refreshAlbumList();
                Toast.makeText(this, "Album created", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showRenameAlbumDialog(Album album, int position) {
        DialogAlbumNameBinding dialogBinding = DialogAlbumNameBinding.inflate(getLayoutInflater());
        TextInputEditText editText = dialogBinding.etAlbumName;
        editText.setText(album.getName());
        editText.selectAll();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename Album")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Rename", null) // Set to null, will override
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newName = editText.getText() != null ? editText.getText().toString().trim() : "";
                
                // Validation: non-empty
                if (newName.isEmpty()) {
                    editText.setError("Album name cannot be empty");
                    return;
                }
                
                // Validation: unique (case-insensitive)
                if (!repository.renameAlbum(album.getName(), newName)) {
                    editText.setError("Album already exists");
                    return;
                }
                
                // Success: refresh and dismiss
                refreshAlbumList();
                Toast.makeText(this, "Album renamed", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
        editText.requestFocus();
    }

    private void showDeleteAlbumDialog(Album album, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete \"" + album.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (repository.deleteAlbum(album.getName())) {
                        refreshAlbumList();
                        Toast.makeText(this, "Album deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Refresh album list from repository and update adapter.
     */
    private void refreshAlbumList() {
        List<Album> updatedAlbums = repository.getAlbums();
        adapter.updateAlbums(updatedAlbums);
    }

    // ==================== DEBUG MENU ACTIONS ====================
    
    private void showLoadTestDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Load Test Data")
                .setMessage("This will clear existing data and load sample albums and photos for testing.\n\nNote: Photos will have placeholder URIs. You'll need to add real photos manually.")
                .setPositiveButton("Load", (dialog, which) -> {
                    repository.loadTestData();
                    refreshAlbumList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will delete ALL albums, photos, and tags. This cannot be undone!")
                .setPositiveButton("Clear", (dialog, which) -> {
                    repository.clearAllData();
                    refreshAlbumList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDataStats() {
        String stats = repository.getDataStats();
        new AlertDialog.Builder(this)
                .setTitle("Data Statistics")
                .setMessage(stats)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list in case albums or photo counts changed
        refreshAlbumList();
    }
}
