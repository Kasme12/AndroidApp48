# Author

Esmeralda Bencosme eb1024

Armaan Saleem As3932

# Photo Album App - GenAI Usage

This document outlines how Google Gemini, a large language model, was used to assist in the development of this Android application.

# Video show all the funcionalitis

https://youtu.be/w7l3R4VXs-c

## Prompts and Features Developed

The development process involved a sequence of prompts to build the app feature by feature. The following is a summary of the prompts and the corresponding functionalities generated or assisted by the AI.

### 1. Home Screen and Album Management
- **Prompt:** "Set up the home screen to display a list of albums."
- **AI Contribution:** Generated the `activity_main.xml` layout with a `RecyclerView` and `FloatingActionButton`. Created the `MainActivity.java` code to populate the list and the `AlbumAdapter` to bind the data.
- **Prompt:** "Add functionality to create, open, delete, and rename albums."
- **AI Contribution:** Implemented the dialog for creating new albums. Added a three-dot menu to each album item in the `AlbumAdapter` and the corresponding handler logic in `MainActivity` for renaming and deleting albums.

### 2. Album and Photo Viewing
- **Prompt:** "Once an album is open, you should be able to add, remove, or display a photo."
- **AI Contribution:** Created the `AlbumActivity` to display photos within an album. It generated the layout with a `RecyclerView` for photos and a `FloatingActionButton` to add new photos from the device gallery. Implemented the `Photo` model class and `PhotoAdapter`.
- **Prompt:** "The photo display screen should include an option for a slideshow."
- **AI Contribution:** Created the `PhotoActivity` to display a single photo. Implemented the slideshow functionality with "Previous" and "Next" buttons to cycle through photos in an album.

### 3. Photo Tagging
- **Prompt:** "When a photo is displayed, you should be able to add and delete tags (Person/Location)."
- **AI Contribution:** Added an "Add Tag" button to `PhotoActivity`. Created a dialog for adding tags with a `Spinner` for the tag type and an `EditText` for the value. Implemented the `Tag` model and `TagAdapter`. Added a three-dot menu next to each tag to allow for deletion.

### 4. Move Photos
- **Prompt:** "You should be able to move a photo from one album to another, with an option to create a new album in the process."
- **AI Contribution:** Implemented a long-press context menu on photos in `AlbumActivity`. Added a dialog that lists existing albums and an option to create a new one. Wrote the logic to move the photo file between album data files.

### 5. Photo Search
- **Prompt:** "You should be able to search for photos by tag-value pairs (single, AND, OR) with auto-completion."
- **AI Contribution:** Added a search icon to the main screen's toolbar. Created a `SearchActivity` with UI for constructing search queries. Implemented logic to scan all photos and provide auto-complete suggestions for tag values. Wrote the search algorithm to handle single-tag, conjunctive (AND), and disjunctive (OR) searches.
- **Prompt:** "There should be functionality to create an album containing the search results."
- **AI Contribution:** Created a `SearchResultsActivity` to display search results. Added a button to create a new album from the results and implemented the corresponding file-saving logic.

### 6. Data Persistence and Bug Fixes
- **Prompts:** Implicitly required or in response to crashes.
- **AI Contribution:** Implemented data serialization to save album and photo data to the device's internal storage. Fixed numerous bugs, including `ClassCastException` from data corruption, `Uri` serialization issues when passing data between activities, and UI layout problems.

---

*This README file was generated with the assistance of the GenAI to document its own involvement in the project.*
