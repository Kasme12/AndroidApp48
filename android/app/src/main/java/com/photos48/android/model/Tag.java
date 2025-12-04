package com.photos48.android.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Tag represents a (type, value) pair attached to a photo.
 * Type is enum: PERSON or LOCATION only.
 * Case-insensitive comparisons for both type and value.
 */
public class Tag {
    
    public enum TagType {
        PERSON,
        LOCATION;
        
        public static TagType fromString(String str) {
            String upper = str.toUpperCase();
            if (upper.equals("PERSON")) return PERSON;
            if (upper.equals("LOCATION")) return LOCATION;
            throw new IllegalArgumentException("Invalid tag type: " + str);
        }
        
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
    
    private TagType type;
    private String value;
    
    public Tag(TagType type, String value) {
        this.type = type;
        this.value = value;
    }
    
    /**
     * Create tag from string type (for convenience).
     */
    public Tag(String typeStr, String value) {
        this.type = TagType.fromString(typeStr);
        this.value = value;
    }
    
    public TagType getTypeEnum() {
        return type;
    }
    
    public String getType() {
        return type.toString();
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Case-insensitive equality check.
     */
    public boolean equalsIgnoreCase(Tag other) {
        if (other == null) return false;
        return this.type == other.type && 
               this.value.equalsIgnoreCase(other.value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return type == tag.type && value.equals(tag.value);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return type.toString() + ": " + value;
    }
    
    /**
     * Convert to JSON object.
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", type.toString());
        json.put("value", value);
        return json;
    }
    
    /**
     * Create Tag from JSON object.
     */
    public static Tag fromJSON(JSONObject json) throws JSONException {
        String typeStr = json.getString("type");
        String value = json.getString("value");
        return new Tag(typeStr, value);
    }
}
