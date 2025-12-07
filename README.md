# Photos App - Android 

## 👥 Authors
**Esmeralda Bencosme** and **Armaan Saleem**

## 📱 Overview

This Android application is a simplified **photo album manager**, allowing users to:

- Create, rename, and delete albums
- Add, view, move, and delete photos in albums
- Tag photos with keywords (e.g., `person:John`, `location:NYC`)
- Search photos by tags
- Navigate photos within an album

---

## 🛠️ Core Functionality

### 1. **Main Screen (Album List)**
- Displays a list of user-created albums.
- Long-press functionality replaced with single tap:
  - **First Tap**: Select album for rename/delete
  - **Second Tap**: Opens album

#### Actions:
- `Create Album`: Opens dialog to name and save a new album.
- `Rename Album`: Renames selected album and updates data.
- `Delete Album`: Removes selected album and associated photos.
- `Search`: Opens a tag-based photo search screen.

---

### 2. **Album View**
- Displays thumbnails of all photos in the selected album.
- Each photo supports:
  - `Move`: Reassigns photo to a different album.
  - `Delete`: Removes the photo from the album.
- Clicking on a photo opens it in **fullscreen** with navigation and tag options.

---

### 3. **Photo View**
- Shows the selected photo in fullscreen.
- Includes buttons to:
  - Navigate `Previous` / `Next`
  - Add or delete tags (`person:` or `location:` prefix required)
  - View all tags associated with the photo

---

### 4. **Search**
- Users can search photos using tags (e.g., `person:John`, `location:Paris`).
- Displays results in a scrollable grid view.

---

## 💾 Data Persistence

- **Albums**: Stored in `SharedPreferences` under `"albums"` using a `Set<String>` for names.
- **Photos**: Stored under `"photos"` where each album key maps to a `Set<String>` of photo URIs.
- **Tags**: Stored under `"tags"` where each photo URI maps to a `Set<String>` of tags.
