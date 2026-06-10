# Build Instructions

## Prerequisites

### 1. Android Studio
Download and install Android Studio from https://developer.android.com/studio

### 2. Rust Toolchain
Install Rust using rustup:
```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

### 3. Android NDK
Install Android NDK through Android Studio SDK Manager or manually:
```bash
# Add Android targets
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
rustup target add i686-linux-android
```

### 4. Android SDK
Ensure you have Android SDK with API level 34 installed.

## Building the Rust Library

### Step 1: Navigate to Rust directory
```bash
cd /storage/emulated/0/CodeOnTheGoProjects/LibDumper/rust
```

### Step 2: Build for each architecture

#### For arm64-v8a (Android 64-bit ARM):
```bash
# Install the linker
cargo install cargo-ndk

# Build
cargo ndk -t arm64-v8a build --release
```

#### For armeabi-v7a (Android 32-bit ARM):
```bash
cargo ndk -t armeabi-v7a build --release
```

#### For x86_64 (Android 64-bit x86):
```bash
cargo ndk -t x86_64 build --release
```

#### For x86 (Android 32-bit x86):
```bash
cargo ndk -t x86 build --release
```

### Step 3: Copy libraries to jniLibs
```bash
mkdir -p ../app/src/main/jniLibs/arm64-v8a
mkdir -p ../app/src/main/jniLibs/armeabi-v7a
mkdir -p ../app/src/main/jniLibs/x86_64
mkdir -p ../app/src/main/jniLibs/x86

# Copy the compiled libraries
cp target/aarch64-linux-android/release/libNeoLibDumper.so ../app/src/main/jniLibs/arm64-v8a/
cp target/armv7-linux-androideabi/release/libNeoLibDumper.so ../app/src/main/jniLibs/armeabi-v7a/
cp target/x86_64-linux-android/release/libNeoLibDumper.so ../app/src/main/jniLibs/x86_64/
cp target/i686-linux-android/release/libNeoLibDumper.so ../app/src/main/jniLibs/x86/
```

## Building the Android App

### Step 1: Open in Android Studio
Open the project in Android Studio by navigating to:
```
/storage/emulated/0/CodeOnTheGoProjects/LibDumper
```

### Step 2: Sync Gradle
Let Android Studio sync the Gradle files automatically.

### Step 3: Build the APK
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Step 4: Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### Issue: Rust build fails
- Ensure you have the correct Android targets installed
- Check that ANDROID_NDK_HOME environment variable is set
- Verify the NDK version is compatible

### Issue: JNI library not found
- Ensure the .so files are in the correct jniLibs directory structure
- Check that the library name matches what's in System.loadLibrary("NeoLibDumper")

### Issue: Gradle sync fails
- Ensure you have the correct JDK version (17 or higher)
- Check that Android SDK is properly configured
- Try File -> Invalidate Caches and Restart

### Issue: Build fails with missing dependencies
- Run `./gradlew clean` and try again
- Ensure you have internet connection for dependency resolution
- Check that maven repositories are accessible

## Development Notes

### Rust Backend
The Rust backend is compiled as a C dynamic library (cdylib) and linked via JNI. The main entry points are defined in `jni_bridge.rs`.

### JNI Bridge
The JNI bridge in Kotlin (`NativeLibWrapper.kt`) provides static methods that call the native Rust functions. All data is serialized as JSON for transfer between Rust and Kotlin.

### Data Flow
1. User selects .so file via SAF
2. Kotlin loads file and passes path to Rust via JNI
3. Rust parses ELF and extracts symbols
4. Rust reconstructs classes and namespaces
5. Rust generates output files
6. Results are returned to Kotlin as JSON
7. Kotlin saves files to storage

## Performance Notes

- Large libraries (100MB+) are supported
- Parallel processing via rayon in Rust
- Background threads via Kotlin coroutines
- Progress notifications during long operations

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## Code Quality

### Lint Check
```bash
./gradlew lint
```

### Kotlin Format
```bash
./gradlew ktlintFormat
```

## Release Checklist

- [ ] Test on multiple Android versions
- [ ] Test on different architectures (arm64, arm, x86_64, x86)
- [ ] Verify all permissions are requested correctly
- [ ] Test file selection via SAF
- [ ] Test dump generation with various configurations
- [ ] Verify output files are generated correctly
- [ ] Test notification system
- [ ] Test error handling scenarios
- [ ] Verify ProGuard rules don't break JNI
- [ ] Sign the APK with release keystore
- [ ] Test on physical device

## Versioning

Follow semantic versioning:
- Major: Breaking changes
- Minor: New features
- Patch: Bug fixes

Update version in:
- `app/build.gradle.kts` (versionName, versionCode)
- `Cargo.toml` (Rust library version)
- `README.md` (documentation)
