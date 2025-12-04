package com.photos48.android.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Album stores photo IDs and manages photo collections.
 * Structure: id (UUID string), name (String), photoIds (List<String>)
 */
public class Album {
    
    private String id;
    private String name;
    private List<String> photoIds;
    
    /**
     * Create a new album with generated UUID.
     */
    public Album(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.photoIds = new ArrayList<>();
    }
    
    /**
     * Create album from existing data (for JSON deserialization).
     */
    public Album(String id, String name, List<String> photoIds) {
        this.id = id;
        this.name = name;
        this.photoIds = photoIds != null ? photoIds : new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getPhotoIds() {
        return Collections.unmodifiableList(photoIds);
    }
    
    /**
     * Add photo ID to album. Returns true if added, false if already exists.
     */
    public boolean addPhoto(String photoId) {
        if (photoIds.contains(photoId)) {
            return false;
        }
        photoIds.add(photoId);
        return true;
    }
    
    /**
     * Remove photo ID from album. Returns true if removed, false if not found.
     */
    public boolean removePhoto(String photoId) {
        return photoIds.remove(photoId);
    }
    
    public int getPhotoCount() {
        return photoIds.size();
    }
    
    /**
     * Convert to JSON object.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        
        JSONArray photoArray = new JSONArray();
        for (String photoId : photoIds) {
            photoArray.put(photoId);
        }
        json.put("photoIds", photoArray);
        
        return json;
    }
    
    /**
     * Create Album from JSON object.
     */
    public static Album fromJSON(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String name = json.getString("name");
        
        List<String> photoIds = new ArrayList<>();
        JSONArray photoArray = json.getJSONArray("photoIds");
        for (int i = 0; i < photoArray.length(); i++) {
            photoIds.add(photoArray.getString(i));
        }
        
        return new Album(id, name, photoIds);
    }
}
