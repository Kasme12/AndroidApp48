package com.example.photoalbum;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private ArrayList<Photo> photos;
    private PhotoAdapter photoAdapter;
    private ActivityResultLauncher<Intent> pickPhotoLauncher;
    private String albumName;
    private ArrayList<String> allAlbums;
    private ActivityResultLauncher<Intent> viewPhotoLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        albumName = getIntent().getStringExtra("album_name");
        if (albumName != null) {
            setTitle(albumName);
        }

        loadPhotos();
        loadAlbumNames();

        RecyclerView photosRecyclerView = findViewById(R.id.photos_recyclerView);
        photoAdapter = new PhotoAdapter(photos, this);
        photosRecyclerView.setAdapter(photoAdapter);
        photosRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        FloatingActionButton addPhotoFab = findViewById(R.id.add_photo_fab);
        addPhotoFab.setOnClickListener(v -> openGallery());

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri photoUri = result.getData().getData();
                        if (photoUri != null) {
                            final int takeFlags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(photoUri, takeFlags);
                            photos.add(new Photo(photoUri));
                            photoAdapter.notifyItemInserted(photos.size() - 1);
                            savePhotos();
                        }
                    }
                });
        viewPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<Photo> updatedPhotos = (ArrayList<Photo>) result.getData().getSerializableExtra("updated_photos");
                        if (updatedPhotos != null) {
                            photos.clear();
                            photos.addAll(updatedPhotos);
                            savePhotos();
                            photoAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        pickPhotoLauncher.launch(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPhotoClick(int position) {
        Intent intent = new Intent(this, PhotoActivity.class);
        intent.putExtra("photos", photos);
        intent.putExtra("position", position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        viewPhotoLauncher.launch(intent);
    }

    @Override
    public void onPhotoLongClick(final int position) {
        final CharSequence[] items = {"Move", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, (dialog, item) -> {
            if (items[item].equals("Move")) {
                movePhoto(position);
            } else if (items[item].equals("Delete")) {
                deletePhoto(position);
            }
        });
        builder.show();
    }

    private void deletePhoto(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    photos.remove(position);
                    photoAdapter.notifyItemRemoved(position);
                    savePhotos();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void movePhoto(final int position) {
        final List<String> destinationAlbums = new ArrayList<>(allAlbums);
        destinationAlbums.remove(albumName);
        destinationAlbums.add(0, "Create New Album...");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Move to...");
        builder.setItems(destinationAlbums.toArray(new String[0]), (dialog, which) -> {
            if (which == 0) {
                createNewAlbumForMove(position);
            } else {
                String destinationAlbum = destinationAlbums.get(which);
                movePhotoToAlbum(position, destinationAlbum);
            }
        });
        builder.show();
    }

    private void createNewAlbumForMove(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Album");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String newAlbumName = input.getText().toString();
            if (!newAlbumName.isEmpty() && !allAlbums.contains(newAlbumName)) {
                allAlbums.add(newAlbumName);
                saveAlbumNames();
                movePhotoToAlbum(position, newAlbumName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void movePhotoToAlbum(int position, String destinationAlbum) {
        Photo photoToMove = photos.get(position);

        ArrayList<Photo> destPhotos = loadPhotos(destinationAlbum);
        destPhotos.add(photoToMove);
        savePhotos(destinationAlbum, destPhotos);

        photos.remove(position);
        photoAdapter.notifyItemRemoved(position);
        savePhotos();
    }

    private void loadAlbumNames() {
        try {
            FileInputStream fis = openFileInput("albums.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            allAlbums = (ArrayList<String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            allAlbums = new ArrayList<>();
        }
    }

    private void saveAlbumNames() {
        try {
            FileOutputStream fos = openFileOutput("albums.dat", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(allAlbums);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePhotos() {
        savePhotos(albumName, photos);
    }

    private void savePhotos(String albumName, ArrayList<Photo> photos) {
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

    private void loadPhotos() {
        photos = loadPhotos(albumName);
    }

    private ArrayList<Photo> loadPhotos(String albumName) {
        try {
            FileInputStream fis = openFileInput(albumName + ".dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<Photo> photos = (ArrayList<Photo>) ois.readObject();
            ois.close();
            fis.close();
            return photos;
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<>();
        }
    }
}