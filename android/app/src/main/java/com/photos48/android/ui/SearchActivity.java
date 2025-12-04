package com.photos48.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.photos48.android.R;
import com.photos48.android.databinding.ActivitySearchBinding;
import com.photos48.android.model.Photo;
import com.photos48.android.persistence.PhotoRepository;
import com.photos48.android.ui.adapters.SearchResultAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity for searching photos by tags with auto-complete.
 * Features: Search by person/location with AND/OR logic, auto-complete
 */
public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private PhotoRepository repository;
    private SearchResultAdapter adapter;
    private List<Photo> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Photos");
        }

        repository = PhotoRepository.getInstance(this);
        searchResults = new ArrayList<>();

        // Setup RecyclerView
        adapter = new SearchResultAdapter(searchResults, this::onPhotoClick);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerView.setAdapter(adapter);

        // Setup tag type spinners
        String[] tagTypes = {"Person", "Location"};
        ArrayAdapter<String> typeAdapter1 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tagTypes);
        binding.spinnerTagType1.setAdapter(typeAdapter1);

        ArrayAdapter<String> typeAdapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tagTypes);
        binding.spinnerTagType2.setAdapter(typeAdapter2);

        // Setup value autocomplete based on selected type
        binding.spinnerTagType1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = tagTypes[position].toLowerCase();
                setupValueAutocomplete(binding.actvTagValue1, selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerTagType2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = tagTypes[position].toLowerCase();
                setupValueAutocomplete(binding.actvTagValue2, selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup dynamic filtering as user types
        binding.actvTagValue1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String selectedType = tagTypes[binding.spinnerTagType1.getSelectedItemPosition()].toLowerCase();
                updateAutocomplete(binding.actvTagValue1, selectedType, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.actvTagValue2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String selectedType = tagTypes[binding.spinnerTagType2.getSelectedItemPosition()].toLowerCase();
                updateAutocomplete(binding.actvTagValue2, selectedType, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search button
        binding.btnSearch.setOnClickListener(v -> performSearch());

        // Initially hide second tag search
        binding.layoutTag2.setVisibility(View.GONE);
        binding.radioGroupOperator.setVisibility(View.GONE);

        // Toggle second tag visibility
        binding.checkboxMultipleTags.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.layoutTag2.setVisibility(View.VISIBLE);
                binding.radioGroupOperator.setVisibility(View.VISIBLE);
            } else {
                binding.layoutTag2.setVisibility(View.GONE);
                binding.radioGroupOperator.setVisibility(View.GONE);
            }
        });
    }

    private void setupValueAutocomplete(AutoCompleteTextView actvValue, String tagType) {
        List<String> existingValues = repository.getAllTagValues(tagType);
        ArrayAdapter<String> valueAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, existingValues);
        actvValue.setAdapter(valueAdapter);
        actvValue.setThreshold(1);
    }

    /**
     * Update autocomplete suggestions with prefix matching (case-insensitive).
     */
    private void updateAutocomplete(AutoCompleteTextView actvValue, String tagType, String prefix) {
        if (prefix.isEmpty()) {
            // Show all values if empty
            List<String> allValues = repository.getAllTagValues(tagType);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, allValues);
            actvValue.setAdapter(adapter);
        } else {
            // Filter by prefix (case-insensitive)
            List<String> allValues = repository.getAllTagValues(tagType);
            List<String> filtered = new ArrayList<>();
            String lowerPrefix = prefix.toLowerCase();
            
            for (String value : allValues) {
                if (value.toLowerCase().startsWith(lowerPrefix)) {
                    filtered.add(value);
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, filtered);
            actvValue.setAdapter(adapter);
        }
    }

    private void performSearch() {
        String type1 = getSelectedTagType(binding.spinnerTagType1);
        String value1 = binding.actvTagValue1.getText().toString().trim();

        String type2 = getSelectedTagType(binding.spinnerTagType2);
        String value2 = binding.actvTagValue2.getText().toString().trim();

        // Handle empty filters
        boolean hasFilter1 = !value1.isEmpty();
        boolean hasFilter2 = binding.checkboxMultipleTags.isChecked() && !value2.isEmpty();

        if (!hasFilter1 && !hasFilter2) {
            // Show all photos if no filters
            Toast.makeText(this, "No filters provided. Showing all photos.", Toast.LENGTH_SHORT).show();
            searchResults.clear();
            searchResults.addAll(getAllPhotos());
            adapter.notifyDataSetChanged();
            binding.tvResultCount.setText(String.format("Found %d photo(s)", searchResults.size()));
            binding.tvResultCount.setVisibility(View.VISIBLE);
            return;
        }

        searchResults.clear();

        if (hasFilter1 && hasFilter2) {
            // Two tag search with AND/OR
            boolean isAnd = binding.radioAnd.isChecked();
            searchResults.addAll(searchByTwoTags(type1, value1, type2, value2, isAnd));
        } else if (hasFilter1) {
            // Single tag search (first filter)
            searchResults.addAll(searchByTagExact(type1, value1));
        } else if (hasFilter2) {
            // Single tag search (second filter only)
            searchResults.addAll(searchByTagExact(type2, value2));
        }

        adapter.notifyDataSetChanged();
        binding.tvResultCount.setText(String.format("Found %d photo(s)", searchResults.size()));
        binding.tvResultCount.setVisibility(View.VISIBLE);

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedTagType(Spinner spinner) {
        return spinner.getSelectedItem().toString().toLowerCase();
    }

    /**
     * Get all photos across all albums.
     */
    private List<Photo> getAllPhotos() {
        return repository.getAllPhotos();
    }

    /**
     * Search by tag with case-insensitive exact match (not prefix).
     */
    private List<Photo> searchByTagExact(String type, String value) {
        return repository.searchByTagExact(type, value);
    }

    private List<Photo> searchByTwoTags(String type1, String value1, String type2, String value2, boolean isAnd) {
        Set<Photo> set1 = new HashSet<>(searchByTagExact(type1, value1));
        Set<Photo> set2 = new HashSet<>(searchByTagExact(type2, value2));

        List<Photo> results = new ArrayList<>();
        
        if (isAnd) {
            // Intersection (AND) - photo must have both tags
            set1.retainAll(set2);
            results.addAll(set1);
        } else {
            // Union (OR) - photo has either tag
            set1.addAll(set2);
            results.addAll(set1);
        }
        
        return results;
    }

    private void onPhotoClick(Photo photo) {
        // Find which album contains this photo
        String albumName = repository.findAlbumContainingPhoto(photo.getId());
        
        if (albumName == null) {
            Toast.makeText(this, "Photo not found in any album", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get album ID for proper navigation
        com.photos48.android.model.Album album = repository.getAlbum(albumName);
        if (album == null) {
            Toast.makeText(this, "Album not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show dialog with album context
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Open Photo")
                .setMessage("This photo is in album: " + albumName)
                .setPositiveButton("View", (dialog, which) -> {
                    Intent intent = new Intent(this, PhotoViewerActivity.class);
                    intent.putExtra("ALBUM_ID", album.getId());
                    intent.putExtra("ALBUM_NAME", albumName);
                    intent.putExtra("PHOTO_POSITION", repository.getPhotoPositionInAlbum(albumName, photo.getId()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
