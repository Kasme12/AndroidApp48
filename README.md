# Photos48 Android

Android port of the Photos48 photo album management application.

## Project Structure

This project uses **both Maven and Gradle** build systems:
- **Gradle (Kotlin DSL)**: Primary build system recommended for Android Studio
- **Maven**: Alternative build system for integration with existing Maven workflows

## Requirements

- **Android Studio**: Latest version (Hedgehog or newer)
- **JDK**: 17 or higher
- **Android SDK**: API 36 (Android 15)
- **Target Device**: 1080 x 2400 420 dpi (Pixel 6, Medium Phone emulator)
- **Maven** (optional): 3.8+ for Maven builds

## Building with Gradle (Recommended)

### Using Android Studio
1. Open the `android` folder in Android Studio
2. Let Gradle sync complete
3. Click **Build > Make Project** or press `Ctrl+F9`
4. Run on emulator: Click **Run > Run 'app'** or press `Shift+F10`

### Using Command Line
```bash
cd android
./gradlew build              # Build the project
./gradlew assembleDebug      # Create debug APK
./gradlew installDebug       # Install on connected device/emulator
```

## Building with Maven (Alternative)

### Prerequisites
1. Set `ANDROID_HOME` environment variable:
   ```bash
   # Windows (PowerShell)
   $env:ANDROID_HOME = "C:\Users\YourUsername\AppData\Local\Android\Sdk"
   
   # Add to system environment variables for persistence
   [System.Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Users\YourUsername\AppData\Local\Android\Sdk', 'User')
   ```

2. Ensure Android SDK Platform 36 is installed via Android Studio SDK Manager

### Maven Build Commands
```bash
cd android
mvn clean compile            # Compile Java sources
mvn package                  # Build APK
mvn android:deploy           # Install to device/emulator
mvn android:run              # Deploy and run
```

## Project Features

### Implemented (from Assignment Requirements)
- ✅ Single-user photo album management
- ✅ Home screen with album list (15 pts)
- ✅ Album CRUD operations (25 pts)
- ✅ Photo management with thumbnails (25 pts)
- ✅ Tag management (person/location only) (15 pts)
- ✅ Move photo between albums (10 pts)
- ✅ Search with autocomplete and AND/OR logic (30 pts)

### Key Differences from JavaFX Version
- ❌ No admin functionality (single user)
- ❌ No login system
- ❌ No explicit captions (filename used)
- ❌ No date/time display
- ❌ No custom tag types (only person/location)
- ❌ No stock user functionality

## Architecture

### Model Layer (`model/`)
- `Album.java` - Album entity with photo references
- `Photo.java` - Photo entity with image URI and tags
- `Tag.java` - Tag entity (person/location types only)

### Persistence Layer (`persistence/`)
- `DataStore.java` - Java serialization-based data storage
- Stores data in app's internal storage
- Persists across app restarts

### UI Layer (`ui/`)
- `MainActivity` - Home screen with album list
- `AlbumActivity` - Album photo grid view
- `PhotoViewerActivity` - Photo display with slideshow
- `SearchActivity` - Search interface with autocomplete

## Testing

### Emulator Configuration
1. Open AVD Manager in Android Studio
2. Create device: **Pixel 6** or **Medium Phone**
3. Specifications:
   - Resolution: 1080 x 2400
   - Density: 420 dpi
   - API Level: 36

### Manual Testing Checklist
- [ ] Create/rename/delete albums
- [ ] Add photos from gallery
- [ ] View photos with prev/next navigation
- [ ] Add/remove tags (person/location)
- [ ] Move photo between albums
- [ ] Search with autocomplete
- [ ] Data persists after app restart

## Development Notes

### Build System Selection
- **Use Gradle** for full Android Studio integration and optimal development experience
- **Use Maven** only if you need to integrate with existing Maven-based workflows or CI/CD pipelines

### Maven Limitations
Maven support for Android is community-maintained and may have limitations compared to Gradle:
- Some AndroidX dependencies may not resolve correctly
- Newer Android features may require Gradle
- Build times may be slower

### Permissions
App requests storage permissions at runtime for accessing device photo gallery:
- `READ_MEDIA_IMAGES` (API 33+)
- `READ_EXTERNAL_STORAGE` (API 32 and below)

## Troubleshooting

### Gradle Issues
```bash
# Clear Gradle cache
cd android
./gradlew clean
./gradlew --refresh-dependencies
```

### Maven Issues
```bash
# Verify ANDROID_HOME is set
echo $env:ANDROID_HOME  # PowerShell

# Clean and rebuild
mvn clean install -U
```

### Android Studio Sync Issues
1. File > Invalidate Caches / Restart
2. File > Sync Project with Gradle Files
3. Build > Clean Project
4. Build > Rebuild Project

## Assignment Compliance

This project fulfills Assignment 4 requirements:
- ✅ Java language (not Kotlin)
- ✅ Kotlin DSL build.gradle.kts
- ✅ Target API 36
- ✅ Tested on 1080 x 2400 420 dpi emulator
- ✅ All required features implemented
- ✅ Data persistence across sessions
- ✅ No third-party image libraries (Picasso)
- ✅ Android XML layouts (not FXML)

## Authors

Esmeralda Bencosme

## License

Academic project for CS 213 - Rutgers University
