package com.example.photoalbum;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AlbumAdapter.OnItemClickListener {

    private ArrayList<String> albums;
    private AlbumAdapter adapter;
    private static final String ALBUMS_FILE = "albums.dat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        setTitle("Albums");

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        albums = new ArrayList<>();
        adapter = new AlbumAdapter(albums, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("New Album");

            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String newAlbum = input.getText().toString();
                albums.add(newAlbum);
                adapter.notifyItemInserted(albums.size() - 1);
                saveAlbums();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlbums();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAlbumClick(int position) {
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra("album_name", albums.get(position));
        startActivity(intent);
    }

    @Override
    public void onRenameAlbum(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Album");
        final EditText input = new EditText(this);
        final String oldName = albums.get(position);
        input.setText(oldName);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString();

            File oldFile = new File(getFilesDir(), oldName + ".dat");
            File newFile = new File(getFilesDir(), newName + ".dat");
            if (oldFile.exists()) {
                oldFile.renameTo(newFile);
            }

            albums.set(position, newName);
            adapter.notifyItemChanged(position);
            saveAlbums();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onDeleteAlbum(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete this album?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String albumNameToDelete = albums.get(position);
                    File file = new File(getFilesDir(), albumNameToDelete + ".dat");
                    if (file.exists()) {
                        file.delete();
                    }

                    albums.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveAlbums();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveAlbums() {
        try {
            FileOutputStream fos = openFileOutput(ALBUMS_FILE, MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(albums);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAlbums() {
        try {
            FileInputStream fis = openFileInput(ALBUMS_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<?> rawList = (ArrayList<?>) ois.readObject();
            ois.close();
            fis.close();

            // Check for data corruption
            boolean isCorrupt = false;
            for (Object obj : rawList) {
                if (!(obj instanceof String)) {
                    isCorrupt = true;
                    break;
                }
            }

            if (isCorrupt) {
                throw new ClassCastException("Corrupted data found");
            }

            albums.clear();
            for(Object obj : rawList) {
                albums.add((String) obj);
            }

        } catch (Exception e) { // Catches file not found, class cast, etc.
            albums.clear();
            saveAlbums(); // Create a new, empty, non-corrupt file
        }
    }
}