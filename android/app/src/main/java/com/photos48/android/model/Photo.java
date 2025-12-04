package com.photos48.android.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Photo model represents a single photo in the system.
 * Structure: id (UUID string), uri (String), tags (List<Tag>)
 * Filename serves as caption, no date field required.
 */
public class Photo {
    
    private String id;
    private String uri;
    private List<Tag> tags;
    
    /**
     * Create a new photo with generated UUID.
     */
    public Photo(String uri) {
        this.id = UUID.randomUUID().toString();
        this.uri = uri;
        this.tags = new ArrayList<>();
    }
    
    /**
     * Create photo from existing data (for JSON deserialization).
     */
    public Photo(String id, String uri, List<Tag> tags) {
        this.id = id;
        this.uri = uri;
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getUri() {
        return uri;
    }
    
    /**
     * Get filename from URI (used as caption).
     */
    public String getFilename() {
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < uri.length() - 1) {
            return uri.substring(lastSlash + 1);
        }
        return uri;
    }
    
    public List<Tag> getTags() {
        return tags;
    }
    
    /**
     * Add tag to photo. Returns true if added, false if duplicate.
     */
    public boolean addTag(Tag tag) {
        // Check for duplicate (case-insensitive)
        for (Tag existingTag : tags) {
            if (existingTag.equalsIgnoreCase(tag)) {
                return false;
            }
        }
        tags.add(tag);
        return true;
    }
    
    /**
     * Remove tag from photo. Returns true if removed, false if not found.
     */
    public boolean removeTag(Tag tag) {
        // Case-insensitive removal
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).equalsIgnoreCase(tag)) {
                tags.remove(i);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if photo has a tag matching the given type and value prefix (case-insensitive).
     */
    public boolean hasTagMatching(String type, String valuePrefix) {
        String lowerPrefix = valuePrefix.toLowerCase();
        for (Tag tag : tags) {
            if (tag.getType().equalsIgnoreCase(type) && 
                tag.getValue().toLowerCase().startsWith(lowerPrefix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Convert to JSON object.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("uri", uri);
        
        JSONArray tagArray = new JSONArray();
        for (Tag tag : tags) {
            tagArray.put(tag.toJSON());
        }
        json.put("tags", tagArray);
        
        return json;
    }
    
    /**
     * Create Photo from JSON object.
     */
    public static Photo fromJSON(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String uri = json.getString("uri");
        
        List<Tag> tags = new ArrayList<>();
        JSONArray tagArray = json.getJSONArray("tags");
        for (int i = 0; i < tagArray.length(); i++) {
            tags.add(Tag.fromJSON(tagArray.getJSONObject(i)));
        }
        
        return new Photo(id, uri, tags);
    }
}
