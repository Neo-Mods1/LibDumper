use jni::JNIEnv;
use jni::objects::{JClass, JObjectArray, JString, JObject, JValue};
use jni::sys::{jboolean, jfloat, jint, jlong, jstring};
use std::sync::Mutex;

use crate::elf_parser::ElfParser;
use crate::symbol_extractor::SymbolExtractor;
use crate::class_reconstructor::ClassReconstructor;
use crate::namespace_detector::NamespaceDetector;
use crate::json_exporter::JsonExporter;
use crate::types::{DumpConfig, DumpResult, ElfInfo, Symbol, ClassInfo, NamespaceInfo};

lazy_static::lazy_static! {
    static ref PARSER: Mutex<ElfParser> = Mutex::new(ElfParser::new());
    static ref EXTRACTOR: SymbolExtractor = SymbolExtractor::new();
    static ref RECONSTRUCTOR: ClassReconstructor = ClassReconstructor::new();
    static ref DETECTOR: NamespaceDetector = NamespaceDetector::new();
    static ref EXPORTER: JsonExporter = JsonExporter::new();
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeLoadElf(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jstring {
    let path: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid path string");
            return std::ptr::null_mut();
        }
    };
    
    let mut parser = match PARSER.lock() {
        Ok(p) => p,
        Err(_) => {
            let _ = env.throw_new("java/lang/RuntimeException", "Failed to acquire parser lock");
            return std::ptr::null_mut();
        }
    };
    
    match parser.load_file(&path) {
        Ok(_) => {},
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", format!("Failed to load ELF: {}", e));
            return std::ptr::null_mut();
        }
    }
    
    match parser.get_elf_info(&path) {
        Ok(info) => {
            match serde_json::to_string(&info) {
                Ok(json) => {
                    let output = match env.new_string(&json) {
                        Ok(s) => s,
                        Err(_) => {
                            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
                            return std::ptr::null_mut();
                        }
                    };
                    output.into_raw()
                }
                Err(e) => {
                    let _ = env.throw_new("java/lang/RuntimeException", format!("JSON serialization failed: {}", e));
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", format!("Failed to get ELF info: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeValidateElf(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jboolean {
    let path: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    
    if ElfParser::is_elf_file(&path) {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeExtractSymbols(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    extract_symtab: jboolean,
    extract_dynsym: jboolean,
    extract_exported: jboolean,
    extract_imported: jboolean,
    dump_raw_names: jboolean,
    generate_cpp_reconstruction: jboolean,
    group_methods_into_classes: jboolean,
    group_static_methods: jboolean,
    detect_constructors: jboolean,
    detect_destructors: jboolean,
    detect_overloaded_methods: jboolean,
    detect_namespaces: jboolean,
    generate_comments: jboolean,
    include_method_signatures: jboolean,
    include_return_types: jboolean,
    include_parameter_types: jboolean,
    attempt_inheritance_detection: jboolean,
    include_virtual_addresses: jboolean,
    include_rva: jboolean,
    include_file_offsets: jboolean,
    include_symbol_sizes: jboolean,
    include_section_names: jboolean,
    generate_dump_cpp: jboolean,
    generate_symbol_table: jboolean,
    generate_credits: jboolean,
    generate_dump_info: jboolean,
    generate_json: jboolean,
) -> jstring {
    let path: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid path string");
            return std::ptr::null_mut();
        }
    };
    
    let config = DumpConfig {
        extract_symtab: extract_symtab != 0,
        extract_dynsym: extract_dynsym != 0,
        extract_exported: extract_exported != 0,
        extract_imported: extract_imported != 0,
        dump_raw_names: dump_raw_names != 0,
        generate_cpp_reconstruction: generate_cpp_reconstruction != 0,
        group_methods_into_classes: group_methods_into_classes != 0,
        group_static_methods: group_static_methods != 0,
        detect_constructors: detect_constructors != 0,
        detect_destructors: detect_destructors != 0,
        detect_overloaded_methods: detect_overloaded_methods != 0,
        detect_namespaces: detect_namespaces != 0,
        generate_comments: generate_comments != 0,
        include_method_signatures: include_method_signatures != 0,
        include_return_types: include_return_types != 0,
        include_parameter_types: include_parameter_types != 0,
        attempt_inheritance_detection: attempt_inheritance_detection != 0,
        include_virtual_addresses: include_virtual_addresses != 0,
        include_rva: include_rva != 0,
        include_file_offsets: include_file_offsets != 0,
        include_symbol_sizes: include_symbol_sizes != 0,
        include_section_names: include_section_names != 0,
        generate_dump_cpp: generate_dump_cpp != 0,
        generate_symbol_table: generate_symbol_table != 0,
        generate_credits: generate_credits != 0,
        generate_dump_info: generate_dump_info != 0,
        generate_json: generate_json != 0,
    };
    
    let mut parser = match PARSER.lock() {
        Ok(p) => p,
        Err(_) => {
            let _ = env.throw_new("java/lang/RuntimeException", "Failed to acquire parser lock");
            return std::ptr::null_mut();
        }
    };
    
    if let Err(e) = parser.load_file(&path) {
        let _ = env.throw_new("java/io/IOException", format!("Failed to load ELF: {}", e));
        return std::ptr::null_mut();
    }
    
    let elf = match parser.get_elf() {
        Some(e) => e,
        None => {
            let _ = env.throw_new("java/io/IOException", "ELF not loaded");
            return std::ptr::null_mut();
        }
    };
    
    let data = parser.get_data();
    
    match EXTRACTOR.extract_symbols(elf, data, &config) {
        Ok(symbols) => {
            match serde_json::to_string(&symbols) {
                Ok(json) => {
                    let output = match env.new_string(&json) {
                        Ok(s) => s,
                        Err(_) => {
                            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
                            return std::ptr::null_mut();
                        }
                    };
                    output.into_raw()
                }
                Err(e) => {
                    let _ = env.throw_new("java/lang/RuntimeException", format!("JSON serialization failed: {}", e));
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Symbol extraction failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeReconstructClasses(
    mut env: JNIEnv,
    _class: JClass,
    symbols_json: JString,
    generate_cpp: jboolean,
    group_methods: jboolean,
    group_static: jboolean,
    detect_constructors: jboolean,
    detect_destructors: jboolean,
    detect_overloaded: jboolean,
    attempt_inheritance: jboolean,
) -> jstring {
    let symbols_str: String = match env.get_string(&symbols_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid symbols JSON");
            return std::ptr::null_mut();
        }
    };
    
    let symbols: Vec<Symbol> = match serde_json::from_str(&symbols_str) {
        Ok(s) => s,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse symbols: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let config = DumpConfig {
        generate_cpp_reconstruction: generate_cpp != 0,
        group_methods_into_classes: group_methods != 0,
        group_static_methods: group_static != 0,
        detect_constructors: detect_constructors != 0,
        detect_destructors: detect_destructors != 0,
        detect_overloaded_methods: detect_overloaded != 0,
        attempt_inheritance_detection: attempt_inheritance != 0,
        ..Default::default()
    };
    
    match RECONSTRUCTOR.reconstruct_classes(&symbols, &config) {
        Ok(classes) => {
            match serde_json::to_string(&classes) {
                Ok(json) => {
                    let output = match env.new_string(&json) {
                        Ok(s) => s,
                        Err(_) => {
                            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
                            return std::ptr::null_mut();
                        }
                    };
                    output.into_raw()
                }
                Err(e) => {
                    let _ = env.throw_new("java/lang/RuntimeException", format!("JSON serialization failed: {}", e));
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Class reconstruction failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeDetectNamespaces(
    mut env: JNIEnv,
    _class: JClass,
    symbols_json: JString,
    classes_json: JString,
    detect_namespaces: jboolean,
) -> jstring {
    let symbols_str: String = match env.get_string(&symbols_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid symbols JSON");
            return std::ptr::null_mut();
        }
    };
    
    let classes_str: String = match env.get_string(&classes_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid classes JSON");
            return std::ptr::null_mut();
        }
    };
    
    let symbols: Vec<Symbol> = match serde_json::from_str(&symbols_str) {
        Ok(s) => s,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse symbols: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let classes: Vec<ClassInfo> = match serde_json::from_str(&classes_str) {
        Ok(c) => c,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse classes: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let config = DumpConfig {
        detect_namespaces: detect_namespaces != 0,
        ..Default::default()
    };
    
    match DETECTOR.detect_namespaces(&symbols, &classes, &config) {
        Ok(namespaces) => {
            match serde_json::to_string(&namespaces) {
                Ok(json) => {
                    let output = match env.new_string(&json) {
                        Ok(s) => s,
                        Err(_) => {
                            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
                            return std::ptr::null_mut();
                        }
                    };
                    output.into_raw()
                }
                Err(e) => {
                    let _ = env.throw_new("java/lang/RuntimeException", format!("JSON serialization failed: {}", e));
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Namespace detection failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeGenerateDumpCpp(
    mut env: JNIEnv,
    _class: JClass,
    classes_json: JString,
    namespaces_json: JString,
    generate_comments: jboolean,
    include_method_signatures: jboolean,
    include_virtual_addresses: jboolean,
    include_rva: jboolean,
    include_file_offsets: jboolean,
    include_symbol_sizes: jboolean,
    include_section_names: jboolean,
    detect_namespaces: jboolean,
) -> jstring {
    let classes_str: String = match env.get_string(&classes_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid classes JSON");
            return std::ptr::null_mut();
        }
    };
    
    let namespaces_str: String = match env.get_string(&namespaces_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid namespaces JSON");
            return std::ptr::null_mut();
        }
    };
    
    let classes: Vec<ClassInfo> = match serde_json::from_str(&classes_str) {
        Ok(c) => c,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse classes: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let namespaces: Vec<NamespaceInfo> = match serde_json::from_str(&namespaces_str) {
        Ok(n) => n,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse namespaces: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let config = DumpConfig {
        generate_comments: generate_comments != 0,
        include_method_signatures: include_method_signatures != 0,
        include_virtual_addresses: include_virtual_addresses != 0,
        include_rva: include_rva != 0,
        include_file_offsets: include_file_offsets != 0,
        include_symbol_sizes: include_symbol_sizes != 0,
        include_section_names: include_section_names != 0,
        detect_namespaces: detect_namespaces != 0,
        ..Default::default()
    };
    
    let output = RECONSTRUCTOR.generate_cpp_output(&classes, &namespaces, &config);
    
    match env.new_string(&output) {
        Ok(s) => s.into_raw(),
        Err(_) => {
            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeGenerateSymbolTable(
    mut env: JNIEnv,
    _class: JClass,
    symbols_json: JString,
    include_virtual_addresses: jboolean,
    include_rva: jboolean,
    include_file_offsets: jboolean,
    include_symbol_sizes: jboolean,
    include_section_names: jboolean,
) -> jstring {
    let symbols_str: String = match env.get_string(&symbols_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid symbols JSON");
            return std::ptr::null_mut();
        }
    };
    
    let symbols: Vec<Symbol> = match serde_json::from_str(&symbols_str) {
        Ok(s) => s,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse symbols: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let output: String = symbols.iter()
        .map(|s| {
            let mut parts = vec![s.name.clone()];
            if include_rva != 0 && s.rva != 0 {
                parts.push(format!("RVA:0x{:X}", s.rva));
            }
            if include_virtual_addresses != 0 && s.address != 0 {
                parts.push(format!("VA:0x{:X}", s.address));
            }
            if include_file_offsets != 0 && s.file_offset != 0 {
                parts.push(format!("Offset:0x{:X}", s.file_offset));
            }
            if include_symbol_sizes != 0 && s.size != 0 {
                parts.push(format!("Size:0x{:X}", s.size));
            }
            if include_section_names != 0 && !s.section.is_empty() && s.section != "UNKNOWN" {
                parts.push(format!("Section:{}", s.section));
            }
            parts.join(" ")
        })
        .collect::<Vec<_>>()
        .join("\n");
    
    match env.new_string(&output) {
        Ok(s) => s.into_raw(),
        Err(_) => {
            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeGenerateJsonExport(
    mut env: JNIEnv,
    _class: JClass,
    elf_info_json: JString,
    symbols_json: JString,
    classes_json: JString,
    namespaces_json: JString,
) -> jstring {
    let elf_info_str: String = match env.get_string(&elf_info_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid ELF info JSON");
            return std::ptr::null_mut();
        }
    };
    
    let symbols_str: String = match env.get_string(&symbols_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid symbols JSON");
            return std::ptr::null_mut();
        }
    };
    
    let classes_str: String = match env.get_string(&classes_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid classes JSON");
            return std::ptr::null_mut();
        }
    };
    
    let namespaces_str: String = match env.get_string(&namespaces_json) {
        Ok(s) => s.into(),
        Err(_) => {
            let _ = env.throw_new("java/io/IOException", "Invalid namespaces JSON");
            return std::ptr::null_mut();
        }
    };
    
    let elf_info: ElfInfo = match serde_json::from_str(&elf_info_str) {
        Ok(e) => e,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse ELF info: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let symbols: Vec<Symbol> = match serde_json::from_str(&symbols_str) {
        Ok(s) => s,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse symbols: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let classes: Vec<ClassInfo> = match serde_json::from_str(&classes_str) {
        Ok(c) => c,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse classes: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let namespaces: Vec<NamespaceInfo> = match serde_json::from_str(&namespaces_str) {
        Ok(n) => n,
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("Failed to parse namespaces: {}", e));
            return std::ptr::null_mut();
        }
    };
    
    let result = DumpResult {
        elf_info,
        symbols,
        classes,
        namespaces,
        dump_cpp: None,
        symbol_table: None,
        dump_info: None,
        credits: None,
        json_export: None,
        total_symbols: 0,
        total_classes: 0,
        total_methods: 0,
        total_namespaces: 0,
        dump_duration_ms: 0,
    };
    
    let config = DumpConfig::default();
    
    match EXPORTER.export(&result, &config) {
        Ok(json) => {
            match env.new_string(&json) {
                Ok(s) => s.into_raw(),
                Err(_) => {
                    let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", format!("JSON export failed: {}", e));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_neomods_libdumper_jni_NativeLibWrapper_00024Companion_nativeGetVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "1.0.0";
    match env.new_string(version) {
        Ok(s) => s.into_raw(),
        Err(_) => {
            let _ = env.throw_new("java/lang/RuntimeException", "Failed to create string");
            std::ptr::null_mut()
        }
    }
}
