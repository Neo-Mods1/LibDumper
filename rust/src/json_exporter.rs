use anyhow::Result;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

use crate::types::{DumpResult, DumpConfig, ElfInfo, Symbol, ClassInfo, NamespaceInfo};

#[derive(Debug, Serialize, Deserialize)]
pub struct JsonExport {
    pub metadata: JsonMetadata,
    pub elf_info: ElfInfo,
    pub symbols: Vec<Symbol>,
    pub classes: Vec<ClassInfo>,
    pub namespaces: Vec<NamespaceInfo>,
    pub statistics: JsonStatistics,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct JsonMetadata {
    pub version: String,
    pub generator: String,
    pub timestamp: String,
    pub dump_config: DumpConfig,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct JsonStatistics {
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_symbols: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub exported_symbols: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub imported_symbols: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub function_symbols: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub object_symbols: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_classes: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_methods: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_constructors: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_destructors: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_static_methods: usize,
    #[serde(deserialize_with = "crate::types::deserialize_usize_from_number")]
    pub total_namespaces: usize,
    #[serde(deserialize_with = "crate::types::deserialize_u64_from_number")]
    pub dump_duration_ms: u64,
}

pub struct JsonExporter;

impl JsonExporter {
    pub fn new() -> Self {
        Self
    }

    pub fn export(
        &self,
        result: &DumpResult,
        config: &DumpConfig,
    ) -> Result<String> {
        let export = JsonExport {
            metadata: JsonMetadata {
                version: "1.0.0".to_string(),
                generator: "Lib Dumper".to_string(),
                timestamp: chrono::Utc::now().to_rfc3339(),
                dump_config: config.clone(),
            },
            elf_info: result.elf_info.clone(),
            symbols: result.symbols.clone(),
            classes: result.classes.clone(),
            namespaces: result.namespaces.clone(),
            statistics: self.build_statistics(result),
        };
        
        let json = serde_json::to_string_pretty(&export)?;
        Ok(json)
    }

    pub fn export_compact(
        &self,
        result: &DumpResult,
        config: &DumpConfig,
    ) -> Result<String> {
        let export = JsonExport {
            metadata: JsonMetadata {
                version: "1.0.0".to_string(),
                generator: "Lib Dumper".to_string(),
                timestamp: chrono::Utc::now().to_rfc3339(),
                dump_config: config.clone(),
            },
            elf_info: result.elf_info.clone(),
            symbols: result.symbols.clone(),
            classes: result.classes.clone(),
            namespaces: result.namespaces.clone(),
            statistics: self.build_statistics(result),
        };
        
        let json = serde_json::to_string(&export)?;
        Ok(json)
    }

    pub fn to_value(&self, result: &DumpResult, config: &DumpConfig) -> Result<Value> {
        let export = JsonExport {
            metadata: JsonMetadata {
                version: "1.0.0".to_string(),
                generator: "Lib Dumper".to_string(),
                timestamp: chrono::Utc::now().to_rfc3339(),
                dump_config: config.clone(),
            },
            elf_info: result.elf_info.clone(),
            symbols: result.symbols.clone(),
            classes: result.classes.clone(),
            namespaces: result.namespaces.clone(),
            statistics: self.build_statistics(result),
        };
        
        let value = serde_json::to_value(&export)?;
        Ok(value)
    }

    fn build_statistics(&self, result: &DumpResult) -> JsonStatistics {
        let exported = result.symbols.iter()
            .filter(|s| s.is_exported)
            .count();
        
        let imported = result.symbols.iter()
            .filter(|s| s.is_imported)
            .count();
        
        let functions = result.symbols.iter()
            .filter(|s| s.is_function)
            .count();
        
        let objects = result.symbols.iter()
            .filter(|s| s.is_object)
            .count();
        
        let total_methods: usize = result.classes.iter()
            .map(|c| c.methods.len() + c.constructors.len() + c.destructors.len() + c.static_methods.len())
            .sum();
        
        let total_constructors: usize = result.classes.iter()
            .map(|c| c.constructors.len())
            .sum();
        
        let total_destructors: usize = result.classes.iter()
            .map(|c| c.destructors.len())
            .sum();
        
        let total_static: usize = result.classes.iter()
            .map(|c| c.static_methods.len())
            .sum();
        
        JsonStatistics {
            total_symbols: result.total_symbols,
            exported_symbols: exported,
            imported_symbols: imported,
            function_symbols: functions,
            object_symbols: objects,
            total_classes: result.total_classes,
            total_methods,
            total_constructors,
            total_destructors,
            total_static_methods: total_static,
            total_namespaces: result.total_namespaces,
            dump_duration_ms: result.dump_duration_ms,
        }
    }

    pub fn format_symbol_json(&self, symbol: &Symbol) -> Value {
        json!({
            "name": symbol.name,
            "demangled_name": symbol.demangled_name,
            "address": format!("0x{:X}", symbol.address),
            "rva": format!("0x{:X}", symbol.rva),
            "file_offset": format!("0x{:X}", symbol.file_offset),
            "size": symbol.size,
            "section": symbol.section,
            "type": symbol.symbol_type,
            "binding": symbol.binding,
            "is_exported": symbol.is_exported,
            "is_imported": symbol.is_imported,
            "is_function": symbol.is_function,
            "is_object": symbol.is_object,
        })
    }

    pub fn format_class_json(&self, class: &ClassInfo) -> Value {
        let methods: Vec<Value> = class.methods.iter()
            .map(|m| self.format_method_json(m))
            .collect();
        
        let constructors: Vec<Value> = class.constructors.iter()
            .map(|m| self.format_method_json(m))
            .collect();
        
        let destructors: Vec<Value> = class.destructors.iter()
            .map(|m| self.format_method_json(m))
            .collect();
        
        let static_methods: Vec<Value> = class.static_methods.iter()
            .map(|m| self.format_method_json(m))
            .collect();
        
        json!({
            "name": class.name,
            "namespace": class.namespace,
            "full_path": class.full_path,
            "base_class": class.base_class,
            "derived_classes": class.derived_classes,
            "methods": methods,
            "constructors": constructors,
            "destructors": destructors,
            "static_methods": static_methods,
        })
    }

    pub fn format_method_json(&self, method: &crate::types::MethodInfo) -> Value {
        json!({
            "name": method.name,
            "demangled_name": method.demangled_name,
            "address": format!("0x{:X}", method.address),
            "rva": format!("0x{:X}", method.rva),
            "file_offset": format!("0x{:X}", method.file_offset),
            "size": method.size,
            "section": method.section,
            "return_type": method.return_type,
            "parameters": method.parameters,
            "is_static": method.is_static,
            "is_virtual": method.is_virtual,
            "is_const": method.is_const,
            "is_overloaded": method.is_overloaded,
            "overload_index": method.overload_index,
        })
    }

    pub fn format_namespace_json(&self, namespace: &NamespaceInfo) -> Value {
        json!({
            "name": namespace.name,
            "full_path": namespace.full_path,
            "parent": namespace.parent,
            "sub_namespaces": namespace.sub_namespaces,
            "classes": namespace.classes,
            "functions": namespace.functions,
        })
    }
}
