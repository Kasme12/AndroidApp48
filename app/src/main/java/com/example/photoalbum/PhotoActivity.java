package com.example.photoalbum;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PhotoActivity extends AppCompatActivity implements TagAdapter.OnTagInteractionListener {

    private ArrayList<Photo> photos;
    private int currentPosition;
    private ImageView photoView;
    private TagAdapter tagAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        Toolbar toolbar = findViewById(R.id.photo_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        photos = (ArrayList<Photo>) getIntent().getSerializableExtra("photos");
        currentPosition = getIntent().getIntExtra("position", 0);

        photoView = findViewById(R.id.photo_view);

        Button prevButton = findViewById(R.id.prev_button);
        prevButton.setOnClickListener(v -> showPreviousPhoto());

        Button nextButton = findViewById(R.id.next_button);
        nextButton.setOnClickListener(v -> showNextPhoto());

        updatePhoto();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.photo_menu, menu);
        return true;
    }

    private void updatePhoto() {
        if (photos != null && !photos.isEmpty()) {
            Photo photo = photos.get(currentPosition);
            photoView.setImageURI(photo.getUri());
            setTitle("Photo " + (currentPosition + 1) + " of " + photos.size());

            RecyclerView tagsRecyclerView = findViewById(R.id.tags_recycler_view);
            tagAdapter = new TagAdapter(photo.getTags(), this);
            tagsRecyclerView.setAdapter(tagAdapter);
            tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            Button addTagButton = findViewById(R.id.add_tag_button);
            addTagButton.setOnClickListener(v -> addTagDialog());
        }
    }

    private void showPreviousPhoto() {
        if (currentPosition > 0) {
            currentPosition--;
            updatePhoto();
        }
    }

    private void showNextPhoto() {
        if (currentPosition < photos.size() - 1) {
            currentPosition++;
            updatePhoto();
        }
    }

    private void addTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_tag, null);
        builder.setView(dialogView);

        final Spinner tagTypeSpinner = dialogView.findViewById(R.id.tag_type_spinner);
        final EditText tagValueEditText = dialogView.findViewById(R.id.tag_value_edittext);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Person", "Location"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(adapter);

        builder.setTitle("Add Tag")
                .setPositiveButton("Add", (dialog, which) -> {
                    String type = (String) tagTypeSpinner.getSelectedItem();
                    String value = tagValueEditText.getText().toString();
                    if (!value.isEmpty()) {
                        photos.get(currentPosition).addTag(new Tag(type, value));
                        tagAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.create().show();
    }

    @Override
    public void onDeleteTag(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Tag")
                .setMessage("Are you sure you want to delete this tag?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    photos.get(currentPosition).getTags().remove(position);
                    tagAdapter.notifyItemRemoved(position);
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("updated_photos", photos);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
            return true;
        } else if (itemId == R.id.action_delete_photo) {
            deleteCurrentPhoto();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentPhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    photos.remove(currentPosition);
                    if (photos.isEmpty()) {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("updated_photos", photos);
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    } else if (currentPosition >= photos.size()) {
                        currentPosition = photos.size() - 1;
                    }
                    updatePhoto();
                })
                .setNegativeButton("No", null)
                .show();
    }
}