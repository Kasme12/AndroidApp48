package com.example.photosapp;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.text.TextWatcher;
import android.text.Editable;

import java.util.*;

public class SearchActivity extends AppCompatActivity {

    private AutoCompleteTextView searchInput;

    private GridView searchResults;
    private ArrayList<String> allMatchingPhotos;
    private ArrayList<String> allTagValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResults);
        allMatchingPhotos = new ArrayList<>();
        allTagValues = new ArrayList<>();

        // Collect all unique tag values from all photos
        SharedPreferences tagPrefs = getSharedPreferences("tags", MODE_PRIVATE);
        Set<String> uniqueValues = new HashSet<>();

        for (Map.Entry<String, ?> entry : tagPrefs.getAll().entrySet()) {
            Set<String> tags = (Set<String>) entry.getValue();
            for (String tag : tags) {
                // Extract value from "person:John" or "location:NYC"
                if (tag.contains(":")) {
                    String value = tag.substring(tag.indexOf(":") + 1).trim();
                    if (!value.isEmpty()) {
                        uniqueValues.add(value);
                    }
                }
            }
        }

        allTagValues.addAll(uniqueValues);
        Collections.sort(allTagValues);

        // Custom adapter that filters based on substring matching
        ArrayAdapter<String> suggestionAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        ) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        if (constraint == null || constraint.length() == 0) {
                            results.values = new ArrayList<>();
                            results.count = 0;
                        } else {
                            String input = constraint.toString().toLowerCase().trim();
                            ArrayList<String> filtered = new ArrayList<>();
                            
                            for (String value : allTagValues) {
                                if (value.toLowerCase().startsWith(input)) {
                                    filtered.add(value);
                                }
                            }
                            results.values = filtered;
                            results.count = filtered.size();
                        }
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results.count > 0) {
                            addAll((ArrayList<String>) results.values);
                            notifyDataSetChanged();
                        } else {
                            notifyDataSetInvalidated();
                        }
                    }
                };
            }
        };

        searchInput.setAdapter(suggestionAdapter);
        searchInput.setThreshold(1); // Show suggestions after 1 character

        // Existing search button logic
        Button searchBtn = findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v -> runSearch());
    }


    private void runSearch() {
        allMatchingPhotos.clear();
        String query = searchInput.getText().toString().trim().toLowerCase();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a tag search query", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("photos", MODE_PRIVATE);
        SharedPreferences tagPrefs = getSharedPreferences("tags", MODE_PRIVATE);
        Map<String, ?> allAlbums = prefs.getAll();

        Set<String> results = new HashSet<>();

        // Split into OR components
        String[] orParts = query.split("(?i)\\s+or\\s+"); // case-insensitive "or"

        for (Object albumUrisObj : allAlbums.values()) {
            if (albumUrisObj instanceof Set) {
                Set<String> albumUris = (Set<String>) albumUrisObj;

                for (String uri : albumUris) {
                    Set<String> tags = tagPrefs.getStringSet(uri, new HashSet<>());

                    boolean matchesOr = false;

                    for (String orPart : orParts) {
                        // For each OR part, check all required AND tags
                        String[] andParts = orPart.trim().split("\\s+");
                        boolean matchesAnd = true;

                        for (String andTag : andParts) {
                            if (!tagMatches(tags, andTag.trim())) {
                                matchesAnd = false;
                                break;
                            }
                        }

                        if (matchesAnd) {
                            matchesOr = true;
                            break;
                        }
                    }

                    if (matchesOr) {
                        results.add(uri);
                    }
                }
            }
        }

        allMatchingPhotos.addAll(results);
        searchResults.setAdapter(new PhotoResultAdapter());
    }


    private boolean tagMatches(Set<String> tags, String query) {
        String normalizedQuery = query.replaceAll("\\s*:\\s*", ":").trim().toLowerCase();

        for (String tag : tags) {
            String normalizedTag = tag.replaceAll("\\s*:\\s*", ":").trim().toLowerCase();
            if (normalizedTag.contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }



    private class PhotoResultAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return allMatchingPhotos.size();
        }

        @Override
        public Object getItem(int position) {
            return allMatchingPhotos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(SearchActivity.this);
            imageView.setImageURI(Uri.parse(allMatchingPhotos.get(position)));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new GridView.LayoutParams(300, 300));
            return imageView;
        }
    }
}
