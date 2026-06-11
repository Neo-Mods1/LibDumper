# Lib Dumper

A professional Android application for analyzing ELF shared libraries and reconstructing C++ symbol information from compiled native code.

---

## Overview

Lib Dumper reads `.so` files (Android native shared libraries), extracts symbol tables, demangles C++ names, and generates clean, human-readable C++ reconstruction files. It is built for reverse engineers, security researchers, and developers who need to understand the internal structure of compiled native libraries.

---

## Features

### ELF Parsing

- Full ELF32 and ELF64 format support
- Automatic architecture detection (ARM, ARM64, x86, x86_64)
- Endianness and file type detection
- Shared library validation

### Symbol Extraction

- Extract symbols from `.symtab` (static symbol table)
- Extract symbols from `.dynsym` (dynamic symbol table)
- Extract exported symbols (globally visible)
- Extract imported symbols (externally referenced)
- Toggle between raw mangled names and demangled C++ names

### C++ Name Demangling

- Full C++ name demangling
- Class name extraction from mangled symbols
- Namespace parsing from qualified names
- Constructor and destructor detection
- Static method identification
- Overloaded method detection and grouping

### Class Reconstruction

- Automatic grouping of methods into their owning classes
- Constructor and destructor classification
- Static method separation
- Overload grouping
- Inheritance relationship detection
- Namespace-aware class organization

### Namespace Detection

- Hierarchical namespace tree construction
- Parent-child namespace relationships
- Class-to-namespace mapping

### C++ Output Generation

- Clean `Dump.cpp` with reconstructed class declarations
- Method signatures with return types and parameters
- Optional address comments (virtual address, RVA, file offset, size, section)
- Namespace wrapping with proper formatting
- Alphabetical class ordering

### Symbol Table Output

- Flat symbol list with configurable detail level
- Optional address information per symbol (VA, RVA, offset, size, section)

### JSON Export

- Complete structured export of all analysis data
- Metadata with timestamps and configuration used
- Statistics summary (symbol counts, class counts, method counts)

---

## Configuration

All dump options are configurable through the UI and persist across sessions.

### Symbol Sources

| Option | Description |
|--------|-------------|
| Extract .symtab | Include symbols from the static symbol table |
| Extract .dynsym | Include symbols from the dynamic symbol table |
| Exported symbols | Include globally visible symbols |
| Imported symbols | Include externally referenced symbols |
| Raw symbol names | Use mangled names instead of demangled names |

### Reconstruction

| Option | Description |
|--------|-------------|
| C++ reconstruction | Generate C++ class declarations from symbols |
| Group methods into classes | Organize methods by their owning class |
| Group static methods | Separate static methods from instance methods |
| Detect constructors | Identify and classify constructor methods |
| Detect destructors | Identify and classify destructor methods |
| Detect overloaded methods | Group methods with the same name |
| Detect namespaces | Build namespace hierarchy from symbol names |
| Generate comments | Add address and size info as comments in Dump.cpp |
| Method signatures | Show full signatures vs. just method names |
| Return types | Include return type information |
| Parameter types | Include parameter type information |
| Inheritance detection | Attempt to detect class inheritance relationships |

### Address Information

| Option | Description |
|--------|-------------|
| Virtual addresses | Include absolute virtual memory addresses |
| RVA | Include relative virtual addresses (offset from image base) |
| File offsets | Include binary file offsets |
| Symbol sizes | Include symbol sizes in bytes |
| Section names | Include the ELF section each symbol belongs to |

### Output Files

| Option | Description |
|--------|-------------|
| Dump.cpp | Generate the C++ reconstruction file |
| SymbolTable.txt | Generate the symbol table file |
| Credits.txt | Generate the credits file |
| DumpInfo.txt | Generate the dump information file |
| JSON export | Generate the structured JSON export |

---

## Usage

1. Install the APK on an Android device running API 29 or higher
2. Grant storage permissions when prompted
3. Tap "Select Library" and navigate to a `.so` file using the system file picker
4. Review the detected ELF information (architecture, file size, validity)
5. Configure dump options using the expandable config cards
6. Tap "Generate Dump" to begin analysis
7. Monitor progress via the dialog or notification
8. Find output files at the configured dump location (default: `/storage/emulated/0/Dumper/`)

### Recent Libraries

Previously analyzed libraries appear below the file picker for quick re-analysis. Tap any entry to reload it without browsing again.

### Settings

- **Theme Mode** -- Switch between System, Light, and Dark themes
- **Language** -- Choose from 10 supported languages
- **Dump Location** -- Configure where output files are saved

---

## Output Files

| File | Description |
|------|-------------|
| `Dump.cpp` | Reconstructed C++ class and namespace declarations |
| `SymbolTable.txt` | Symbol names with optional address information |
| `DumpInfo.txt` | Analysis metadata, file info, and statistics |
| `Credits.txt` | Application credits and contact information |
| `dump.json` | Full structured JSON export |

---

## Supported Languages

English, Spanish, Portuguese, French, German, Japanese, Chinese (Simplified), Russian, Arabic, Indonesian.

---

## Requirements

- Android 10 (API 29) or higher
- No root required
- Storage permission for file access

---

## License

MIT License

---

## Disclaimer

Use this application responsibly. The developer is not responsible for misuse of generated output. Always ensure you have proper authorization before analyzing third-party software.
