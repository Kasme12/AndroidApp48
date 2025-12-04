# Photos48 Android - Build Instructions

## Quick Start

### Option 1: Gradle (Recommended for Android Development)
```powershell
cd android
.\gradlew build
.\gradlew installDebug  # Install on connected device/emulator
```

### Option 2: Maven (Alternative Build System)
```powershell
# First, set ANDROID_HOME environment variable
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Build with Maven
cd android
.\mvnw.cmd clean package
.\mvnw.cmd android:deploy
```

## Environment Setup

### 1. Install Android SDK
- Install Android Studio from https://developer.android.com/studio
- Open SDK Manager and install:
  - Android SDK Platform 36
  - Android SDK Build-Tools
  - Android SDK Platform-Tools
  - Android SDK Tools

### 2. Set ANDROID_HOME (for Maven builds)
```powershell
# Temporary (current session only)
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Permanent (add to system environment)
[System.Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
```

### 3. Verify Setup
```powershell
# Check ANDROID_HOME
echo $env:ANDROID_HOME

# Check ADB is accessible
& "$env:ANDROID_HOME\platform-tools\adb.exe" version
```

## Maven Build Commands

### Compilation
```powershell
.\mvnw.cmd clean compile  # Clean and compile Java sources
```

### Packaging
```powershell
.\mvnw.cmd package  # Build APK file
```

### Deployment
```powershell
# Start emulator first, then:
.\mvnw.cmd android:deploy  # Install APK on device/emulator
.\mvnw.cmd android:run     # Deploy and launch app
```

### Full Build Cycle
```powershell
.\mvnw.cmd clean package android:deploy android:run
```

## Gradle Build Commands

### Basic Build
```powershell
.\gradlew build           # Full build
.\gradlew assembleDebug   # Build debug APK
.\gradlew assembleRelease # Build release APK
```

### Installation & Running
```powershell
.\gradlew installDebug  # Install debug APK
.\gradlew uninstallAll  # Uninstall from device
```

### Cleaning
```powershell
.\gradlew clean         # Clean build outputs
.\gradlew --refresh-dependencies  # Refresh dependencies
```

## Troubleshooting

### Maven: "Cannot find Android SDK"
```powershell
# Verify ANDROID_HOME is set correctly
echo $env:ANDROID_HOME

# Should output something like:
# C:\Users\YourName\AppData\Local\Android\Sdk
```

### Maven: Dependency Resolution Issues
```powershell
# Clear Maven cache and rebuild
.\mvnw.cmd dependency:purge-local-repository
.\mvnw.cmd clean install -U
```

### Gradle: Sync Failed
```powershell
# In Android Studio:
# File > Invalidate Caches / Restart

# Or clean and rebuild:
.\gradlew clean build --refresh-dependencies
```

### ADB: Device Not Found
```powershell
# List connected devices
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices

# Start ADB server
& "$env:ANDROID_HOME\platform-tools\adb.exe" start-server
```

## IDE Integration

### Android Studio (Recommended)
1. Open Android Studio
2. File > Open > Select `android` folder
3. Wait for Gradle sync to complete
4. Build > Make Project (Ctrl+F9)
5. Run > Run 'app' (Shift+F10)

### VS Code
1. Install extensions:
   - Extension Pack for Java
   - Android for VS Code (optional)
2. Open `android` folder
3. Use terminal for Maven/Gradle commands

### IntelliJ IDEA
1. Open `android` folder as project
2. Select "Import Gradle project"
3. Use built-in Gradle/Maven tools

## Build System Comparison

| Feature | Gradle | Maven |
|---------|--------|-------|
| Android Studio Support | ✅ Full | ⚠️ Limited |
| Build Speed | ✅ Fast | ⚠️ Slower |
| Dependency Resolution | ✅ Excellent | ⚠️ AAR issues |
| Official Android Support | ✅ Yes | ❌ No |
| **Recommendation** | **Primary** | **Alternative** |

## Recommended Workflow

For **active development**:
- Use **Gradle** with Android Studio
- Better IDE integration and debugging

For **CI/CD or Maven integration**:
- Use **Maven** if you have existing Maven infrastructure
- Note: May require additional configuration for complex builds
