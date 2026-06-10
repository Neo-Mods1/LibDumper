use serde::{Deserialize, Serialize, Deserializer};

pub fn deserialize_u64_from_number<'de, D>(deserializer: D) -> Result<u64, D::Error>
where
    D: Deserializer<'de>,
{
    let val = serde_json::Value::deserialize(deserializer)?;
    match val {
        serde_json::Value::Number(n) => {
            if let Some(u) = n.as_u64() {
                Ok(u)
            } else if let Some(f) = n.as_f64() {
                Ok(f as u64)
            } else {
                Err(serde::de::Error::custom(format!("cannot convert number to u64: {}", n)))
            }
        }
        serde_json::Value::String(s) => {
            s.parse::<u64>().map_err(serde::de::Error::custom)
        }
        _ => Err(serde::de::Error::custom(format!("expected number or string for u64, got: {}", val))),
    }
}

pub fn deserialize_usize_from_number<'de, D>(deserializer: D) -> Result<usize, D::Error>
where
    D: Deserializer<'de>,
{
    let val = serde_json::Value::deserialize(deserializer)?;
    match val {
        serde_json::Value::Number(n) => {
            if let Some(u) = n.as_u64() {
                Ok(u as usize)
            } else if let Some(f) = n.as_f64() {
                Ok(f as usize)
            } else {
                Err(serde::de::Error::custom(format!("cannot convert number to usize: {}", n)))
            }
        }
        serde_json::Value::String(s) => {
            s.parse::<usize>().map_err(serde::de::Error::custom)
        }
        _ => Err(serde::de::Error::custom(format!("expected number or string for usize, got: {}", val))),
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ElfInfo {
    pub file_name: String,
    pub file_path: String,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub file_size: u64,
    pub architecture: String,
    pub bit_width: u32,
    pub endian: String,
    pub elf_type: String,
    pub machine: String,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub entry_point: u64,
    pub is_valid: bool,
    pub is_shared_library: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Symbol {
    pub name: String,
    pub demangled_name: String,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub address: u64,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub rva: u64,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub file_offset: u64,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub size: u64,
    pub section: String,
    #[serde(deserialize_with = "deserialize_usize_from_number")]
    pub section_index: usize,
    pub symbol_type: String,
    pub binding: String,
    pub is_exported: bool,
    pub is_imported: bool,
    pub is_function: bool,
    pub is_object: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClassInfo {
    pub name: String,
    pub namespace: String,
    pub full_path: String,
    pub methods: Vec<MethodInfo>,
    pub constructors: Vec<MethodInfo>,
    pub destructors: Vec<MethodInfo>,
    pub static_methods: Vec<MethodInfo>,
    pub overloaded_methods: Vec<Vec<MethodInfo>>,
    pub base_class: Option<String>,
    pub derived_classes: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MethodInfo {
    pub name: String,
    pub demangled_name: String,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub address: u64,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub rva: u64,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub file_offset: u64,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub size: u64,
    pub section: String,
    pub return_type: Option<String>,
    pub parameters: Vec<String>,
    pub is_static: bool,
    pub is_virtual: bool,
    pub is_const: bool,
    pub is_overloaded: bool,
    #[serde(deserialize_with = "deserialize_usize_from_number")]
    pub overload_index: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NamespaceInfo {
    pub name: String,
    pub full_path: String,
    pub parent: Option<String>,
    pub sub_namespaces: Vec<String>,
    pub classes: Vec<String>,
    pub functions: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DumpConfig {
    pub extract_symtab: bool,
    pub extract_dynsym: bool,
    pub extract_exported: bool,
    pub extract_imported: bool,
    pub dump_raw_names: bool,
    pub generate_cpp_reconstruction: bool,
    pub group_methods_into_classes: bool,
    pub group_static_methods: bool,
    pub detect_constructors: bool,
    pub detect_destructors: bool,
    pub detect_overloaded_methods: bool,
    pub detect_namespaces: bool,
    pub generate_comments: bool,
    pub include_method_signatures: bool,
    pub include_return_types: bool,
    pub include_parameter_types: bool,
    pub attempt_inheritance_detection: bool,
    pub include_virtual_addresses: bool,
    pub include_rva: bool,
    pub include_file_offsets: bool,
    pub include_symbol_sizes: bool,
    pub include_section_names: bool,
    pub generate_dump_cpp: bool,
    pub generate_symbol_table: bool,
    pub generate_credits: bool,
    pub generate_dump_info: bool,
    pub generate_json: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DumpResult {
    pub elf_info: ElfInfo,
    pub symbols: Vec<Symbol>,
    pub classes: Vec<ClassInfo>,
    pub namespaces: Vec<NamespaceInfo>,
    pub dump_cpp: Option<String>,
    pub symbol_table: Option<String>,
    pub dump_info: Option<String>,
    pub credits: Option<String>,
    pub json_export: Option<String>,
    #[serde(deserialize_with = "deserialize_usize_from_number")]
    pub total_symbols: usize,
    #[serde(deserialize_with = "deserialize_usize_from_number")]
    pub total_classes: usize,
    #[serde(deserialize_with = "deserialize_usize_from_number")]
    pub total_methods: usize,
    #[serde(deserialize_with = "deserialize_usize_from_number")]
    pub total_namespaces: usize,
    #[serde(deserialize_with = "deserialize_u64_from_number")]
    pub dump_duration_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DumpProgress {
    pub stage: String,
    pub progress: f32,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ErrorInfo {
    pub code: i32,
    pub message: String,
    pub details: Option<String>,
}

impl Default for DumpConfig {
    fn default() -> Self {
        Self {
            extract_symtab: true,
            extract_dynsym: true,
            extract_exported: true,
            extract_imported: true,
            dump_raw_names: true,
            generate_cpp_reconstruction: true,
            group_methods_into_classes: true,
            group_static_methods: true,
            detect_constructors: true,
            detect_destructors: true,
            detect_overloaded_methods: true,
            detect_namespaces: true,
            generate_comments: true,
            include_method_signatures: true,
            include_return_types: true,
            include_parameter_types: true,
            attempt_inheritance_detection: true,
            include_virtual_addresses: true,
            include_rva: true,
            include_file_offsets: true,
            include_symbol_sizes: true,
            include_section_names: true,
            generate_dump_cpp: true,
            generate_symbol_table: true,
            generate_credits: true,
            generate_dump_info: true,
            generate_json: true,
        }
    }
}
