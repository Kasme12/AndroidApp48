package com.photos48.android.persistence;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.photos48.android.model.Album;
import com.photos48.android.model.Photo;
import com.photos48.android.model.Tag;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository manages all data operations with JSON persistence.
 * Single JSON file in app-private storage: photos_data.json
 * 
 * Features:
 * - Case-insensitive album name validation
 * - Duplicate prevention for album names
 * - Success/failure return values for all operations
 * - Auto-save after modifications (background I/O)
 * - Corruption recovery (starts fresh if JSON is invalid)
 * - Thread-safe save operations with ExecutorService
 */
public class PhotoRepository {
    
    private static final String TAG = "PhotoRepository";
    private static final String DATA_FILE = "photos_data.json";
    
    private static PhotoRepository instance;
    private Context context;
    
    private List<Album> albums;
    private Map<String, Photo> photoMap; // photoId -> Photo
    
    // Background I/O
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean hasShownCorruptionToast = false;
    
    private PhotoRepository(Context context) {
        this.context = context.getApplicationContext();
        this.albums = new ArrayList<>();
        this.photoMap = new HashMap<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        loadData();
    }
    
    public static synchronized PhotoRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PhotoRepository(context);
        }
        return instance;
    }
    
    // ==================== ALBUM OPERATIONS ====================
    
    /**
     * Get all albums.
     */
    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }
    
    /**
     * Get album by name (case-insensitive).
     */
    public Album getAlbum(String name) {
        for (Album album : albums) {
            if (album.getName().equalsIgnoreCase(name)) {
                return album;
            }
        }
        return null;
    }
    
    /**
     * Get album by ID.
     */
    public Album getAlbumById(String id) {
        for (Album album : albums) {
            if (album.getId().equals(id)) {
                return album;
            }
        }
        return null;
    }
    
    /**
     * Check if album name exists (case-insensitive).
     */
    public boolean albumExists(String name) {
        return getAlbum(name) != null;
    }
    
    /**
     * Add a new album. Returns true if successful, false if duplicate name.
     */
    public boolean addAlbum(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Check for duplicate (case-insensitive)
        if (albumExists(name)) {
            return false;
        }
        
        Album album = new Album(name.trim());
        albums.add(album);
        saveData();
        return true;
    }
    
    /**
     * Rename album. Returns true if successful, false if new name is duplicate or album not found.
     */
    public boolean renameAlbum(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        
        Album album = getAlbum(oldName);
        if (album == null) {
            return false;
        }
        
        // Check if new name already exists (case-insensitive), unless it's the same album
        Album existingAlbum = getAlbum(newName);
        if (existingAlbum != null && !existingAlbum.getId().equals(album.getId())) {
            return false;
        }
        
        album.setName(newName.trim());
        saveData();
        return true;
    }
    
    /**
     * Delete album by name. Returns true if successful, false if not found.
     */
    public boolean deleteAlbum(String name) {
        Album album = getAlbum(name);
        if (album == null) {
            return false;
        }
        
        albums.remove(album);
        saveData();
        return true;
    }
    
    /**
     * Get album names except the specified one.
     */
    public List<String> getAlbumNamesExcept(String excludeName) {
        List<String> names = new ArrayList<>();
        for (Album album : albums) {
            if (!album.getName().equalsIgnoreCase(excludeName)) {
                names.add(album.getName());
            }
        }
        return names;
    }
    
    // ==================== PHOTO OPERATIONS ====================
    
    /**
     * Get photo by ID.
     */
    public Photo getPhoto(String photoId) {
        return photoMap.get(photoId);
    }
    
    /**
     * Get all photos.
     */
    public List<Photo> getAllPhotos() {
        return new ArrayList<>(photoMap.values());
    }
    
    /**
     * Get photos in an album.
     */
    public List<Photo> getPhotosInAlbum(String albumName) {
        Album album = getAlbum(albumName);
        if (album == null) {
            return new ArrayList<>();
        }
        
        List<Photo> photos = new ArrayList<>();
        for (String photoId : album.getPhotoIds()) {
            Photo photo = photoMap.get(photoId);
            if (photo != null) {
                photos.add(photo);
            }
        }
        return photos;
    }
    
    /**
     * Add photo to album. Creates new Photo object with unique ID.
     * Returns photo ID if successful, null if album not found or URI already in album.
     */
    public String addPhotoToAlbum(String albumName, String uri) {
        Album album = getAlbum(albumName);
        if (album == null) {
            return null;
        }
        
        // Check if URI already exists in this album
        for (String photoId : album.getPhotoIds()) {
            Photo existingPhoto = photoMap.get(photoId);
            if (existingPhoto != null && existingPhoto.getUri().equals(uri)) {
                return null; // Duplicate in this album
            }
        }
        
        // Create new photo (each album entry gets unique ID even if same image)
        Photo photo = new Photo(uri);
        photoMap.put(photo.getId(), photo);
        album.addPhoto(photo.getId());
        saveData();
        return photo.getId();
    }
    
    /**
     * Remove photo from album. Returns true if successful, false if not found.
     * Photo is removed from photoMap only if not in any other album.
     */
    public boolean removePhotoFromAlbum(String albumName, String photoId) {
        Album album = getAlbum(albumName);
        if (album == null) {
            return false;
        }
        
        if (!album.removePhoto(photoId)) {
            return false;
        }
        
        // Check if photo is in any other album
        boolean inOtherAlbum = false;
        for (Album a : albums) {
            if (a.getPhotoIds().contains(photoId)) {
                inOtherAlbum = true;
                break;
            }
        }
        
        // If not in any album, remove from photoMap
        if (!inOtherAlbum) {
            photoMap.remove(photoId);
        }
        
        saveData();
        return true;
    }
    
    /**
     * Move photo from one album to another by album IDs. Returns true if successful.
     * The photo entry (with all metadata and tags) is moved, not copied.
     */
    public boolean movePhotoById(String photoId, String fromAlbumId, String toAlbumId) {
        Album fromAlbum = getAlbumById(fromAlbumId);
        Album toAlbum = getAlbumById(toAlbumId);
        
        if (fromAlbum == null || toAlbum == null) {
            return false;
        }
        
        // Prevent moving to same album
        if (fromAlbumId.equals(toAlbumId)) {
            return false;
        }
        
        if (!fromAlbum.removePhoto(photoId)) {
            return false;
        }
        
        toAlbum.addPhoto(photoId);
        saveData();
        return true;
    }
    
    /**
     * Move photo from one album to another. Returns true if successful.
     */
    public boolean movePhoto(String photoId, String fromAlbumName, String toAlbumName) {
        Album fromAlbum = getAlbum(fromAlbumName);
        Album toAlbum = getAlbum(toAlbumName);
        
        if (fromAlbum == null || toAlbum == null) {
            return false;
        }
        
        if (!fromAlbum.removePhoto(photoId)) {
            return false;
        }
        
        toAlbum.addPhoto(photoId);
        saveData();
        return true;
    }
    
    /**
     * Find album name containing photo.
     */
    public String findAlbumContainingPhoto(String photoId) {
        for (Album album : albums) {
            if (album.getPhotoIds().contains(photoId)) {
                return album.getName();
            }
        }
        return null;
    }
    
    /**
     * Get photo position in album.
     */
    public int getPhotoPositionInAlbum(String albumName, String photoId) {
        Album album = getAlbum(albumName);
        if (album != null) {
            return album.getPhotoIds().indexOf(photoId);
        }
        return -1;
    }
    
    // ==================== TAG OPERATIONS ====================
    
    /**
     * Add tag to photo. Returns true if successful, false if duplicate or photo not found.
     */
    public boolean addTagToPhoto(String photoId, Tag tag) {
        Photo photo = photoMap.get(photoId);
        if (photo == null) {
            return false;
        }
        
        boolean added = photo.addTag(tag);
        if (added) {
            saveData();
        }
        return added;
    }
    
    /**
     * Remove tag from photo. Returns true if successful.
     */
    public boolean removeTagFromPhoto(String photoId, Tag tag) {
        Photo photo = photoMap.get(photoId);
        if (photo == null) {
            return false;
        }
        
        boolean removed = photo.removeTag(tag);
        if (removed) {
            saveData();
        }
        return removed;
    }
    
    /**
     * Get all unique tag values for a given type (case-insensitive).
     * Used for autocomplete.
     */
    public List<String> getAllTagValues(String tagType) {
        List<String> values = new ArrayList<>();
        
        for (Photo photo : photoMap.values()) {
            for (Tag tag : photo.getTags()) {
                if (tag.getType().equalsIgnoreCase(tagType)) {
                    String value = tag.getValue();
                    // Add if not already present (case-sensitive for display)
                    if (!values.contains(value)) {
                        values.add(value);
                    }
                }
            }
        }
        
        return values;
    }
    
    /**
     * Search photos by tag (case-insensitive, supports prefix matching).
     */
    public List<Photo> searchByTag(String tagType, String valuePrefix) {
        List<Photo> results = new ArrayList<>();
        
        for (Photo photo : photoMap.values()) {
            if (photo.hasTagMatching(tagType, valuePrefix)) {
                results.add(photo);
            }
        }
        
        return results;
    }
    
    /**
     * Search photos by tag with exact value match (case-insensitive).
     */
    public List<Photo> searchByTagExact(String tagType, String value) {
        List<Photo> results = new ArrayList<>();
        
        for (Photo photo : photoMap.values()) {
            for (Tag tag : photo.getTags()) {
                if (tag.getType().equalsIgnoreCase(tagType) && 
                    tag.getValue().equalsIgnoreCase(value)) {
                    results.add(photo);
                    break; // Only add photo once even if multiple matching tags
                }
            }
        }
        
        return results;
    }
    
    // ==================== PERSISTENCE ====================
    
    /**
     * Save all data to JSON file in background thread.
     * Uses ExecutorService for non-blocking I/O.
     */
    private void saveData() {
        saveDataAsync(null);
    }
    
    /**
     * Save all data to JSON file in background with optional callback.
     * @param onComplete Callback executed on main thread after save (can be null)
     */
    private void saveDataAsync(Runnable onComplete) {
        executorService.execute(() -> {
            try {
                JSONObject root = new JSONObject();
                
                // Save albums
                JSONArray albumsArray = new JSONArray();
                synchronized (albums) {
                    for (Album album : albums) {
                        albumsArray.put(album.toJSON());
                    }
                }
                root.put("albums", albumsArray);
                
                // Save photos
                JSONArray photosArray = new JSONArray();
                synchronized (photoMap) {
                    for (Photo photo : photoMap.values()) {
                        photosArray.put(photo.toJSON());
                    }
                }
                root.put("photos", photosArray);
                
                // Write to file in app-private storage
                File file = new File(context.getFilesDir(), DATA_FILE);
                FileWriter writer = new FileWriter(file);
                writer.write(root.toString(2)); // Pretty print with indent
                writer.close();
                
                Log.d(TAG, "Data saved successfully to " + file.getAbsolutePath());
                
                // Execute callback on main thread if provided
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
                
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Error saving data", e);
                mainHandler.post(() -> 
                    Toast.makeText(context, "Error saving data", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    
    /**
     * Load all data from JSON file on app start.
     * If file is corrupted, starts fresh and shows one-time toast.
     * File I/O is confined to app-private storage.
     */
    private void loadData() {
        File file = new File(context.getFilesDir(), DATA_FILE);
        if (!file.exists()) {
            Log.d(TAG, "No data file found at " + file.getAbsolutePath() + ", starting fresh");
            return;
        }
        
        try {
            // Read file from app-private storage
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
            
            // Parse JSON
            JSONObject root = new JSONObject(json.toString());
            
            // Load photos first (albums reference photo IDs)
            photoMap.clear();
            if (root.has("photos")) {
                JSONArray photosArray = root.getJSONArray("photos");
                for (int i = 0; i < photosArray.length(); i++) {
                    Photo photo = Photo.fromJSON(photosArray.getJSONObject(i));
                    photoMap.put(photo.getId(), photo);
                }
            }
            
            // Load albums
            albums.clear();
            if (root.has("albums")) {
                JSONArray albumsArray = root.getJSONArray("albums");
                for (int i = 0; i < albumsArray.length(); i++) {
                    Album album = Album.fromJSON(albumsArray.getJSONObject(i));
                    albums.add(album);
                }
            }
            
            Log.d(TAG, "Data loaded successfully: " + albums.size() + " albums, " + photoMap.size() + " photos");
            
        } catch (JSONException e) {
            // Corrupted JSON - start fresh and show toast
            Log.e(TAG, "JSON corrupted, starting fresh", e);
            albums = new ArrayList<>();
            photoMap = new HashMap<>();
            
            if (!hasShownCorruptionToast) {
                mainHandler.post(() -> 
                    Toast.makeText(context, "Data file was corrupted. Starting fresh.", Toast.LENGTH_LONG).show()
                );
                hasShownCorruptionToast = true;
            }
            
            // Delete corrupted file
            if (file.exists()) {
                file.delete();
            }
            
        } catch (IOException e) {
            // File read error - start fresh
            Log.e(TAG, "Error reading data file, starting fresh", e);
            albums = new ArrayList<>();
            photoMap = new HashMap<>();
            
            mainHandler.post(() -> 
                Toast.makeText(context, "Error reading data file", Toast.LENGTH_SHORT).show()
            );
        }
    }
    
    // ==================== TEST DATA HELPERS ====================
    
    /**
     * Load sample test data for manual testing.
     * Creates 3 albums with photos and tags for testing all features.
     * Note: Uses placeholder URIs since we can't programmatically add real photos.
     */
    public void loadTestData() {
        // Clear existing data
        albums.clear();
        photoMap.clear();
        
        try {
            // Create albums
            Album vacation = new Album("Vacation 2024");
            Album family = new Album("Family");
            Album work = new Album("Work");
            
            albums.add(vacation);
            albums.add(family);
            albums.add(work);
            
            // Create sample photos with tags
            // NOTE: These use placeholder URIs - real testing requires actual photo selection
            
            // Vacation photos
            Photo photo1 = createSamplePhoto("content://sample/photo1");
            photo1.addTag(new Tag("person", "Alice"));
            photo1.addTag(new Tag("location", "New York"));
            photoMap.put(photo1.getId(), photo1);
            vacation.addPhoto(photo1.getId());
            
            Photo photo2 = createSamplePhoto("content://sample/photo2");
            photo2.addTag(new Tag("person", "Bob"));
            photo2.addTag(new Tag("location", "New York"));
            photoMap.put(photo2.getId(), photo2);
            vacation.addPhoto(photo2.getId());
            
            Photo photo3 = createSamplePhoto("content://sample/photo3");
            photo3.addTag(new Tag("person", "Alice"));
            photo3.addTag(new Tag("location", "Paris"));
            photoMap.put(photo3.getId(), photo3);
            vacation.addPhoto(photo3.getId());
            
            Photo photo4 = createSamplePhoto("content://sample/photo4");
            photo4.addTag(new Tag("person", "Charlie"));
            photo4.addTag(new Tag("location", "Tokyo"));
            photoMap.put(photo4.getId(), photo4);
            vacation.addPhoto(photo4.getId());
            
            Photo photo5 = createSamplePhoto("content://sample/photo5");
            photo5.addTag(new Tag("person", "Alice"));
            photo5.addTag(new Tag("location", "Tokyo"));
            photoMap.put(photo5.getId(), photo5);
            vacation.addPhoto(photo5.getId());
            
            // Family photos
            Photo photo6 = createSamplePhoto("content://sample/photo6");
            photo6.addTag(new Tag("person", "Alice"));
            photo6.addTag(new Tag("person", "Bob"));
            photo6.addTag(new Tag("location", "Home"));
            photoMap.put(photo6.getId(), photo6);
            family.addPhoto(photo6.getId());
            
            Photo photo7 = createSamplePhoto("content://sample/photo7");
            photo7.addTag(new Tag("person", "Charlie"));
            photo7.addTag(new Tag("location", "Home"));
            photoMap.put(photo7.getId(), photo7);
            family.addPhoto(photo7.getId());
            
            Photo photo8 = createSamplePhoto("content://sample/photo8");
            photo8.addTag(new Tag("person", "David"));
            photo8.addTag(new Tag("location", "Beach"));
            photoMap.put(photo8.getId(), photo8);
            family.addPhoto(photo8.getId());
            
            // Work photos
            Photo photo9 = createSamplePhoto("content://sample/photo9");
            photo9.addTag(new Tag("person", "Manager"));
            photo9.addTag(new Tag("location", "Office"));
            photoMap.put(photo9.getId(), photo9);
            work.addPhoto(photo9.getId());
            
            Photo photo10 = createSamplePhoto("content://sample/photo10");
            photo10.addTag(new Tag("person", "Team"));
            photo10.addTag(new Tag("location", "Office"));
            photoMap.put(photo10.getId(), photo10);
            work.addPhoto(photo10.getId());
            
            // Save test data
            saveData();
            
            Log.d(TAG, "Test data loaded: " + albums.size() + " albums, " + photoMap.size() + " photos");
            
            mainHandler.post(() -> 
                Toast.makeText(context, "Test data loaded: 3 albums, 10 photos", Toast.LENGTH_LONG).show()
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading test data", e);
            mainHandler.post(() -> 
                Toast.makeText(context, "Error loading test data", Toast.LENGTH_SHORT).show()
            );
        }
    }
    
    /**
     * Helper to create sample photo with unique ID.
     */
    private Photo createSamplePhoto(String uri) {
        return new Photo(uri);
    }
    
    /**
     * Clear all data (useful for testing fresh start).
     */
    public void clearAllData() {
        albums.clear();
        photoMap.clear();
        saveData();
        
        Log.d(TAG, "All data cleared");
        mainHandler.post(() -> 
            Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
        );
    }
    
    /**
     * Get data statistics for debugging.
     */
    public String getDataStats() {
        int totalPhotos = photoMap.size();
        int totalTags = 0;
        for (Photo photo : photoMap.values()) {
            totalTags += photo.getTags().size();
        }
        
        return String.format("Albums: %d, Photos: %d, Tags: %d", 
            albums.size(), totalPhotos, totalTags);
    }
    
    // ==================== CLEANUP ====================
    
    /**
     * Cleanup resources (call from Application.onTerminate if needed).
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
