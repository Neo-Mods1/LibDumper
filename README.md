# Lib Dumper

A professional Android ELF (.so) analysis and symbol dumping tool.

## Features

- **ELF Parsing**: Full ELF format support with architecture detection
- **Symbol Extraction**: Extract from .symtab, .dynsym, exported, and imported symbols
- **Symbol Demangling**: C++ symbol demangling with cpp_demangle
- **Class Reconstruction**: Detect and reconstruct C++ classes from mangled symbols
- **Namespace Detection**: Identify and organize namespaces
- **Method Grouping**: Group methods by class, detect constructors/destructors
- **C++ Output Generation**: Generate clean C++ reconstruction files
- **JSON Export**: Structured data export for external tooling
- **Modern UI**: Material 3 design with Jetpack Compose
- **Dark/Light Themes**: System, Light, and Dark theme support
- **Progress Notifications**: Real-time dump progress updates
- **Storage Access Framework**: Browse and select files from any storage provider

## Architecture

- **Frontend**: Kotlin, Jetpack Compose, Material 3, MVVM, Hilt DI
- **Backend**: Rust, JNI, goblin, cpp_demangle, anyhow, serde, rayon
- **Storage**: DataStore Preferences, SAF

## Requirements

- Android 10 (API 29) or higher
- Rust toolchain for native library compilation
- Android NDK

## Building

### Prerequisites

1. Install Android Studio
2. Install Rust toolchain: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`
3. Add Android targets:
   ```bash
   rustup target add aarch64-linux-android
   rustup target add armv7-linux-androideabi
   rustup target add x86_64-linux-android
   rustup target add i686-linux-android
   ```

### Build Steps

1. Clone the repository
2. Build the Rust library:
   ```bash
   cd rust
   cargo build --release
   ```
3. Copy the compiled libraries to `app/src/main/jniLibs/`
4. Build the Android app:
   ```bash
   ./gradlew assembleRelease
   ```

## Project Structure

```
LibDumper/
├── app/
│   ├── src/main/
│   │   ├── java/com/neomods/libdumper/
│   │   │   ├── ui/
│   │   │   │   ├── navigation/
│   │   │   │   ├── screens/
│   │   │   │   ├── components/
│   │   │   │   └── theme/
│   │   │   ├── viewmodels/
│   │   │   ├── repository/
│   │   │   ├── domain/
│   │   │   ├── storage/
│   │   │   ├── jni/
│   │   │   └── utils/
│   │   └── res/
│   └── build.gradle.kts
├── rust/
│   ├── src/
│   │   ├── lib.rs
│   │   ├── elf_parser.rs
│   │   ├── symbol_extractor.rs
│   │   ├── demangler.rs
│   │   ├── class_reconstructor.rs
│   │   ├── namespace_detector.rs
│   │   ├── json_exporter.rs
│   │   ├── jni_bridge.rs
│   │   └── types.rs
│   └── Cargo.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Output Files

- **Dump.cpp**: Reconstructed C++ structures with methods, classes, and namespaces
- **SymbolTable.txt**: Raw symbol names, one per line
- **DumpInfo.txt**: Analysis metadata and statistics
- **Credits.txt**: Application credits and usage instructions
- **dump.json**: Structured JSON export for external tooling

## Configuration

The application provides extensive configuration options:

### Symbol Sources
- Extract .symtab
- Extract .dynsym
- Extract exported symbols
- Extract imported symbols
- Dump raw symbol names

### Reconstruction Options
- Generate C++ reconstruction
- Group methods into classes
- Group static methods
- Detect constructors/destructors
- Detect overloaded methods
- Detect namespaces
- Generate comments
- Include method signatures, return types, parameter types
- Attempt inheritance detection

### Address Information
- Include Virtual Addresses
- Include RVA (Relative Virtual Address)
- Include File Offsets
- Include Symbol Sizes
- Include Section Names

### Output Files
- Generate Dump.cpp
- Generate SymbolTable.txt
- Generate Credits.txt
- Generate DumpInfo.txt
- Generate JSON export

## License

MIT License

## Disclaimer

Use this application responsibly. The developer is not responsible for misuse of generated output.
