package com.example.photosapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import java.util.Set;
import java.util.HashSet;
import android.widget.EditText;

import java.util.ArrayList;

public class PhotoViewActivity extends AppCompatActivity {

    private ArrayList<String> photoUris;
    private int currentIndex;
    private ImageView fullscreenImage;
    private TextView tagDisplay;
    private String currentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        fullscreenImage = findViewById(R.id.fullscreenImage);
        Button prevBtn = findViewById(R.id.prevBtn);
        Button nextBtn = findViewById(R.id.nextBtn);

        photoUris = getIntent().getStringArrayListExtra("photoUris");
        currentIndex = getIntent().getIntExtra("position", 0);

        currentPhotoUri = photoUris.get(currentIndex);
        tagDisplay = findViewById(R.id.tagDisplay);


        displayPhoto();

        prevBtn.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                displayPhoto();
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (currentIndex < photoUris.size() - 1) {
                currentIndex++;
                displayPhoto();
            }
        });

        Button addTagBtn = findViewById(R.id.addTagBtn);
        Button deleteTagBtn = findViewById(R.id.deleteTagBtn);

        addTagBtn.setOnClickListener(v -> showTagDialog(true));
        deleteTagBtn.setOnClickListener(v -> showTagDialog(false));

    }

    private void showTagDialog(boolean isAdding) {
        EditText input = new EditText(this);
        input.setHint("person:John or location:NYC");

        new AlertDialog.Builder(this)
                .setTitle(isAdding ? "Add Tag" : "Delete Tag")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String entry = input.getText().toString().replaceAll("\\s*:\\s*", ":").trim().toLowerCase();
                    if (!(entry.startsWith("person:") || entry.startsWith("location:"))) {
                        Toast.makeText(this, "Only person: or location: tags allowed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SharedPreferences prefs = getSharedPreferences("tags", MODE_PRIVATE);
                    Set<String> tags = new HashSet<>(prefs.getStringSet(currentPhotoUri, new HashSet<>()));

                    if (isAdding) {
                        tags.add(entry);
                    } else {
                        tags.remove(entry);
                    }

                    prefs.edit().putStringSet(currentPhotoUri, tags).apply();
                    updateTagsDisplay();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTagsDisplay() {
        Set<String> tags = getSharedPreferences("tags", MODE_PRIVATE).getStringSet(currentPhotoUri, new HashSet<>());
        tagDisplay.setText("Tags: " + String.join(", ", tags));
    }

    private void displayPhoto() {
        try {
            currentPhotoUri = photoUris.get(currentIndex);
            fullscreenImage.setImageURI(Uri.parse(currentPhotoUri));
            updateTagsDisplay();
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
}
