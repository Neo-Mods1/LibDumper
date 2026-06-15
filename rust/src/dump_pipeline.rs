use std::fs::{self, File};
use std::io::{BufWriter, Write};
use std::path::Path;
use anyhow::Result;
use serde::Serialize;

use crate::elf_parser::ElfParser;
use crate::symbol_extractor::SymbolExtractor;
use crate::class_reconstructor::ClassReconstructor;
use crate::namespace_detector::NamespaceDetector;
use crate::json_exporter::JsonExporter;
use crate::types::{DumpConfig, DumpResult};

#[derive(Debug, Serialize)]
pub struct DumpStats {
    pub total_symbols: usize,
    pub total_classes: usize,
    pub total_namespaces: usize,
    pub dump_cpp_written: bool,
    pub symbol_table_written: bool,
    pub json_written: bool,
}

pub fn run_dump(
    lib_path: &str,
    output_dir: &str,
    config: &DumpConfig,
) -> Result<DumpStats> {
    let mut parser = ElfParser::new();
    parser.load_file(lib_path)?;

    let elf = parser.get_elf().ok_or_else(|| anyhow::anyhow!("ELF not loaded"))?;
    let data = parser.get_data();

    let elf_info = parser.get_elf_info(lib_path)?;

    let extractor = SymbolExtractor::new();
    let symbols = extractor.extract_symbols(elf, data, config)?;

    let reconstructor = ClassReconstructor::new();
    let classes = reconstructor.reconstruct_classes(&symbols, config)?;

    let detector = NamespaceDetector::new();
    let namespaces = detector.detect_namespaces(&symbols, &classes, config)?;

    let output_path = Path::new(output_dir);
    fs::create_dir_all(output_path)?;

    let mut dump_cpp_written = false;
    let mut symbol_table_written = false;
    let mut json_written = false;

    if config.generate_dump_cpp {
        let dump_cpp = reconstructor.generate_cpp_output(&classes, &namespaces, config);
        let file = File::create(output_path.join("Dump.cpp"))?;
        let mut writer = BufWriter::new(file);
        writer.write_all(dump_cpp.as_bytes())?;
        writer.flush()?;
        dump_cpp_written = true;
    }

    if config.generate_symbol_table {
        let file = File::create(output_path.join("SymbolTable.txt"))?;
        let mut writer = BufWriter::new(file);
        for s in &symbols {
            let mut parts = vec![s.name.clone()];
            if config.include_rva && s.rva != 0 {
                parts.push(format!("RVA:0x{:X}", s.rva));
            }
            if config.include_virtual_addresses && s.address != 0 {
                parts.push(format!("VA:0x{:X}", s.address));
            }
            if config.include_file_offsets && s.file_offset != 0 {
                parts.push(format!("Offset:0x{:X}", s.file_offset));
            }
            if config.include_symbol_sizes && s.size != 0 {
                parts.push(format!("Size:0x{:X}", s.size));
            }
            if config.include_section_names && !s.section.is_empty() && s.section != "UNKNOWN" {
                parts.push(format!("Section:{}", s.section));
            }
            writeln!(writer, "{}", parts.join(" "))?;
        }
        writer.flush()?;
        symbol_table_written = true;
    }

    let total_symbols = symbols.len();
    let total_classes = classes.len();
    let total_namespaces = namespaces.len();

    if config.generate_json {
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
        let exporter = JsonExporter::new();
        let json = exporter.export(&result, config)?;
        let file = File::create(output_path.join("dump.json"))?;
        let mut writer = BufWriter::new(file);
        writer.write_all(json.as_bytes())?;
        writer.flush()?;
        json_written = true;
    }

    let stats = DumpStats {
        total_symbols,
        total_classes,
        total_namespaces,
        dump_cpp_written,
        symbol_table_written,
        json_written,
    };

    Ok(stats)
}
