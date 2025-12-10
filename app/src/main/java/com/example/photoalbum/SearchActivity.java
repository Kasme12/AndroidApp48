package com.example.photoalbum;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private AutoCompleteTextView tagValue1Autocomplete, tagValue2Autocomplete;
    private Spinner tagType1Spinner, tagType2Spinner, operatorSpinner;
    private Button searchButton;

    private ArrayList<Photo> allPhotos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            setTitle("Search Photos");
        }

        tagType1Spinner = findViewById(R.id.tag_type1_spinner);
        tagValue1Autocomplete = findViewById(R.id.tag_value1_autocomplete);
        operatorSpinner = findViewById(R.id.operator_spinner);
        tagType2Spinner = findViewById(R.id.tag_type2_spinner);
        tagValue2Autocomplete = findViewById(R.id.tag_value2_autocomplete);
        searchButton = findViewById(R.id.search_button);

        loadAllPhotos();
        setupSpinners();
        setupAutoComplete();

        searchButton.setOnClickListener(v -> performSearch());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAllPhotos() {
        allPhotos = new ArrayList<>();
        ArrayList<String> albumNames = loadAlbumNames();
        for (String name : albumNames) {
            allPhotos.addAll(loadPhotos(name));
        }
    }

    private ArrayList<String> loadAlbumNames() {
        ArrayList<String> albumNames = new ArrayList<>();
        try {
            FileInputStream fis = openFileInput("albums.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            albumNames = (ArrayList<String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return albumNames;
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

    private void setupSpinners() {
        ArrayAdapter<String> tagTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Person", "Location"});
        tagTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagType1Spinner.setAdapter(tagTypeAdapter);
        tagType2Spinner.setAdapter(tagTypeAdapter);

        ArrayAdapter<String> operatorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"AND", "OR"});
        operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        operatorSpinner.setAdapter(operatorAdapter);
    }

    private void setupAutoComplete() {
        Set<String> personTags = new HashSet<>();
        Set<String> locationTags = new HashSet<>();

        for (Photo photo : allPhotos) {
            for (Tag tag : photo.getTags()) {
                if (tag.getType().equalsIgnoreCase("Person")) {
                    personTags.add(tag.getValue());
                } else if (tag.getType().equalsIgnoreCase("Location")) {
                    locationTags.add(tag.getValue());
                }
            }
        }

        ArrayAdapter<String> personAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(personTags));
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(locationTags));

        tagValue1Autocomplete.setAdapter(personAdapter); // Default
        tagValue2Autocomplete.setAdapter(personAdapter); // Default

        tagType1Spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) tagValue1Autocomplete.setAdapter(personAdapter); else tagValue1Autocomplete.setAdapter(locationAdapter);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        tagType2Spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) tagValue2Autocomplete.setAdapter(personAdapter); else tagValue2Autocomplete.setAdapter(locationAdapter);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void performSearch() {
        String tagType1 = (String) tagType1Spinner.getSelectedItem();
        String tagValue1 = tagValue1Autocomplete.getText().toString().trim();
        String operator = (String) operatorSpinner.getSelectedItem();
        String tagType2 = (String) tagType2Spinner.getSelectedItem();
        String tagValue2 = tagValue2Autocomplete.getText().toString().trim();

        ArrayList<Photo> results = new ArrayList<>();

        boolean hasClause1 = !tagValue1.isEmpty();
        boolean hasClause2 = !tagValue2.isEmpty();

        for (Photo photo : allPhotos) {
            if (!hasClause1 && !hasClause2) {
                continue; 
            }

            boolean match1 = hasClause1 && checkMatch(photo, tagType1, tagValue1);
            boolean match2 = hasClause2 && checkMatch(photo, tagType2, tagValue2);

            if (hasClause1 && !hasClause2) { 
                if (match1) results.add(photo);
            } else if (!hasClause1 && hasClause2) { 
                if (match2) results.add(photo);
            } else if (hasClause1 && hasClause2) {
                if (operator.equals("AND")) {
                    if (match1 && match2) results.add(photo);
                } else { // OR
                    if (match1 || match2) results.add(photo);
                }
            }
        }

        Intent intent = new Intent(this, SearchResultsActivity.class);
        intent.putExtra("search_results", results);
        startActivity(intent);
    }

    private boolean checkMatch(Photo photo, String type, String value) {
        for (Tag tag : photo.getTags()) {
            if (tag.getType().equalsIgnoreCase(type) && tag.getValue().toLowerCase().startsWith(value.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
