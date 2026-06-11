use anyhow::{Context, Result};
use goblin::elf::Elf;
use goblin::elf::sym::{Sym, STB_GLOBAL, STB_WEAK, STT_FUNC, STT_OBJECT, STT_NOTYPE, STT_SECTION, STT_FILE, STT_COMMON, STT_TLS};

use crate::types::{Symbol, DumpConfig};
use crate::demangler::Demangler;

pub struct SymbolExtractor {
    demangler: Demangler,
}

impl SymbolExtractor {
    pub fn new() -> Self {
        Self {
            demangler: Demangler::new(),
        }
    }

    pub fn extract_symbols(
        &self,
        elf: &Elf,
        data: &[u8],
        config: &DumpConfig,
    ) -> Result<Vec<Symbol>> {
        let mut symbols = Vec::new();
        
        if config.extract_symtab {
            let symtab_symbols = self.extract_from_section(elf, data, ".symtab", true)?;
            symbols.extend(symtab_symbols);
        }
        
        if config.extract_dynsym {
            let dynsym_symbols = self.extract_from_section(elf, data, ".dynsym", false)?;
            symbols.extend(dynsym_symbols);
        }
        
        if config.extract_exported {
            let exported = self.extract_exported(elf, data, config)?;
            symbols.extend(exported);
        }
        
        if config.extract_imported {
            let imported = self.extract_imported(elf, data, config)?;
            symbols.extend(imported);
        }
        
        symbols.sort_by(|a, b| a.address.cmp(&b.address));
        symbols.dedup_by(|a, b| a.name == b.name);
        
        Ok(symbols)
    }

    fn extract_from_section(
        &self,
        elf: &Elf,
        data: &[u8],
        section_name: &str,
        is_symtab: bool,
    ) -> Result<Vec<Symbol>> {
        let mut symbols = Vec::new();
        
        let syms = if is_symtab {
            &elf.syms
        } else {
            &elf.dynsyms
        };
        
        for sym in syms.iter() {
            if sym.st_name == 0 {
                continue;
            }
            
            let name = if is_symtab {
                elf.strtab.get_at(sym.st_name).unwrap_or("").to_string()
            } else {
                elf.dynstrtab.get_at(sym.st_name).unwrap_or("").to_string()
            };
            
            if name.is_empty() {
                continue;
            }
            
            let section = self.get_section_name(elf, sym);
            let file_offset = self.calculate_file_offset(elf, sym, section_name);
            
            let symbol = Symbol {
                name: if config.dump_raw_names { name.clone() } else { self.demangler.demangle(&name) },
                demangled_name: self.demangler.demangle(&name),
                address: sym.st_value,
                rva: if sym.st_value >= self.get_image_base(elf) {
                    sym.st_value - self.get_image_base(elf)
                } else {
                    sym.st_value
                },
                file_offset,
                size: sym.st_size,
                section,
                section_index: sym.st_shndx,
                symbol_type: self.get_symbol_type_name(sym),
                binding: self.get_binding_name(sym),
                is_exported: self.is_exported_symbol(sym),
                is_imported: self.is_imported_symbol(sym, is_symtab),
                is_function: sym.st_type() == STT_FUNC,
                is_object: sym.st_type() == STT_OBJECT,
            };
            
            symbols.push(symbol);
        }
        
        Ok(symbols)
    }

    fn extract_exported(&self, elf: &Elf, _data: &[u8], config: &DumpConfig) -> Result<Vec<Symbol>> {
        let mut symbols = Vec::new();
        
        for sym in elf.dynsyms.iter() {
            if sym.st_name == 0 {
                continue;
            }
            
            if sym.st_bind() == STB_GLOBAL || sym.st_bind() == STB_WEAK {
                if sym.st_value != 0 {
                    let name = elf.dynstrtab.get_at(sym.st_name)
                        .unwrap_or("")
                        .to_string();
                    
                    if !name.is_empty() {
                        let symbol = Symbol {
                            name: if config.dump_raw_names { name.clone() } else { self.demangler.demangle(&name) },
                            demangled_name: self.demangler.demangle(&name),
                            address: sym.st_value,
                            rva: sym.st_value,
                            file_offset: 0,
                            size: sym.st_size,
                            section: ".dynsym".to_string(),
                            section_index: sym.st_shndx,
                            symbol_type: self.get_symbol_type_name(sym),
                            binding: self.get_binding_name(sym),
                            is_exported: true,
                            is_imported: false,
                            is_function: sym.st_type() == STT_FUNC,
                            is_object: sym.st_type() == STT_OBJECT,
                        };
                        symbols.push(symbol);
                    }
                }
            }
        }
        
        Ok(symbols)
    }

    fn extract_imported(&self, elf: &Elf, _data: &[u8], config: &DumpConfig) -> Result<Vec<Symbol>> {
        let mut symbols = Vec::new();
        
        for sym in elf.dynsyms.iter() {
            if sym.st_name == 0 {
                continue;
            }
            
            if sym.st_shndx == goblin::elf::section_header::SHN_UNDEF as usize {
                let name = elf.dynstrtab.get_at(sym.st_name)
                    .unwrap_or("")
                    .to_string();
                
                if !name.is_empty() {
                    let symbol = Symbol {
                        name: if config.dump_raw_names { name.clone() } else { self.demangler.demangle(&name) },
                        demangled_name: self.demangler.demangle(&name),
                        address: 0,
                        rva: 0,
                        file_offset: 0,
                        size: 0,
                        section: ".dynsym".to_string(),
                        section_index: sym.st_shndx,
                        symbol_type: self.get_symbol_type_name(sym),
                        binding: self.get_binding_name(sym),
                        is_exported: false,
                        is_imported: true,
                        is_function: sym.st_type() == STT_FUNC,
                        is_object: sym.st_type() == STT_OBJECT,
                    };
                    symbols.push(symbol);
                }
            }
        }
        
        Ok(symbols)
    }

    fn get_section_name(&self, elf: &Elf, sym: Sym) -> String {
        if sym.st_shndx == goblin::elf::section_header::SHN_UNDEF as usize {
            "UNDEF".to_string()
        } else if sym.st_shndx == goblin::elf::section_header::SHN_ABS as usize {
            "ABS".to_string()
        } else if sym.st_shndx == goblin::elf::section_header::SHN_COMMON as usize {
            "COMMON".to_string()
        } else if sym.st_shndx < elf.section_headers.len() {
            let sh = &elf.section_headers[sym.st_shndx];
            elf.shdr_strtab.get_at(sh.sh_name)
                .unwrap_or("UNKNOWN")
                .to_string()
        } else {
            "UNKNOWN".to_string()
        }
    }

    fn calculate_file_offset(&self, elf: &Elf, sym: Sym, section_name: &str) -> u64 {
        if section_name == "UNDEF" || section_name == "ABS" || section_name == "COMMON" {
            return 0;
        }
        
        if sym.st_shndx < elf.section_headers.len() {
            let sh = &elf.section_headers[sym.st_shndx];
            if sh.sh_type == goblin::elf::section_header::SHT_PROGBITS 
                || sh.sh_type == goblin::elf::section_header::SHT_NOTE 
                || sh.sh_type == goblin::elf::section_header::SHT_INIT_ARRAY 
                || sh.sh_type == goblin::elf::section_header::SHT_FINI_ARRAY {
                return sym.st_value - sh.sh_addr + sh.sh_offset;
            }
        }
        
        sym.st_value
    }

    fn get_image_base(&self, elf: &Elf) -> u64 {
        elf.program_headers.iter()
            .filter(|ph| ph.p_type == goblin::elf::program_header::PT_LOAD)
            .map(|ph| ph.p_vaddr)
            .min()
            .unwrap_or(0)
    }

    fn get_symbol_type_name(&self, sym: Sym) -> String {
        match sym.st_type() {
            STT_NOTYPE => "NOTYPE".to_string(),
            STT_OBJECT => "OBJECT".to_string(),
            STT_FUNC => "FUNC".to_string(),
            STT_SECTION => "SECTION".to_string(),
            STT_FILE => "FILE".to_string(),
            STT_COMMON => "COMMON".to_string(),
            STT_TLS => "TLS".to_string(),
            _ => format!("UNKNOWN({})", sym.st_type()),
        }
    }

    fn get_binding_name(&self, sym: Sym) -> String {
        match sym.st_bind() {
            goblin::elf::sym::STB_LOCAL => "LOCAL".to_string(),
            STB_GLOBAL => "GLOBAL".to_string(),
            STB_WEAK => "WEAK".to_string(),
            _ => format!("UNKNOWN({})", sym.st_bind()),
        }
    }

    fn is_exported_symbol(&self, sym: Sym) -> bool {
        (sym.st_bind() == STB_GLOBAL || sym.st_bind() == STB_WEAK) 
            && sym.st_shndx != goblin::elf::section_header::SHN_UNDEF as usize
    }

    fn is_imported_symbol(&self, sym: Sym, is_symtab: bool) -> bool {
        if is_symtab {
            false
        } else {
            sym.st_shndx == goblin::elf::section_header::SHN_UNDEF as usize
        }
    }
}
