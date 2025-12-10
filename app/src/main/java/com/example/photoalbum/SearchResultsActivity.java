package com.example.photoalbum;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class SearchResultsActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private ArrayList<Photo> searchResults;
    private ArrayList<String> albumNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        Toolbar toolbar = findViewById(R.id.search_results_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            setTitle("Search Results");
        }

        searchResults = (ArrayList<Photo>) getIntent().getSerializableExtra("search_results");
        loadAlbumNames();

        RecyclerView searchResultsRecyclerView = findViewById(R.id.search_results_recyclerView);
        PhotoAdapter photoAdapter = new PhotoAdapter(searchResults, this);
        searchResultsRecyclerView.setAdapter(photoAdapter);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        Button createAlbumButton = findViewById(R.id.create_album_button);
        createAlbumButton.setOnClickListener(v -> createAlbumFromResults());
    }

    private void createAlbumFromResults() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Album Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String albumName = input.getText().toString();
            if (!albumName.isEmpty() && !albumNames.contains(albumName)) {
                albumNames.add(albumName);
                saveAlbumNames();
                savePhotosToAlbum(albumName, searchResults);
                finish(); // Return to the previous screen
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void savePhotosToAlbum(String albumName, ArrayList<Photo> photos) {
        try {
            FileOutputStream fos = openFileOutput(albumName + ".dat", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(photos);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAlbumNames() {
        try {
            FileInputStream fis = openFileInput("albums.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            albumNames = (ArrayList<String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            albumNames = new ArrayList<>();
        }
    }

    private void saveAlbumNames() {
        try {
            FileOutputStream fos = openFileOutput("albums.dat", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(albumNames);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPhotoClick(int position) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("photos", searchResults);
        intent.putExtra("position", position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onPhotoLongClick(int position) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}