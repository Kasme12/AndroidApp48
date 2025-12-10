package com.example.photoalbum;

import android.net.Uri;

import java.io.Serializable;
import java.util.ArrayList;

public class Photo implements Serializable {
    private String uriString;
    private ArrayList<Tag> tags;

    public Photo(Uri uri) {
        this.uriString = uri.toString();
        this.tags = new ArrayList<>();
    }

    public Uri getUri() {
        return Uri.parse(uriString);
    }

    public ArrayList<Tag> getTags() {
        return tags;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
}