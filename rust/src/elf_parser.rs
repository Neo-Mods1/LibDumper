use anyhow::{Context, Result};
use goblin::elf::Elf;
use std::path::Path;
use std::fs;

use crate::types::ElfInfo;

pub struct ElfParser {
    data: Vec<u8>,
    elf: Option<Elf<'static>>,
}

impl ElfParser {
    pub fn new() -> Self {
        Self {
            data: Vec::new(),
            elf: None,
        }
    }

    pub fn load_file(&mut self, path: &str) -> Result<()> {
        self.data = fs::read(path)
            .with_context(|| format!("Failed to read file: {}", path))?;
        
        let data_ptr = self.data.as_ptr() as *const u8;
        let data_len = self.data.len();
        
        unsafe {
            let static_slice = std::slice::from_raw_parts(data_ptr, data_len);
            self.elf = Some(Elf::parse(static_slice)
                .context("Failed to parse ELF header")?);
        }
        
        Ok(())
    }

    pub fn get_elf_info(&self, path: &str) -> Result<ElfInfo> {
        let elf = self.elf.as_ref()
            .context("ELF not loaded")?;
        
        let metadata = fs::metadata(path)
            .with_context(|| format!("Failed to get file metadata: {}", path))?;
        
        let architecture = match elf.header.e_machine {
            goblin::elf::header::EM_386 => "x86".to_string(),
            goblin::elf::header::EM_X86_64 => "x86_64".to_string(),
            goblin::elf::header::EM_ARM => "ARM32".to_string(),
            goblin::elf::header::EM_AARCH64 => "ARM64".to_string(),
            goblin::elf::header::EM_MIPS => "MIPS".to_string(),
            goblin::elf::header::EM_PPC => "PowerPC".to_string(),
            goblin::elf::header::EM_PPC64 => "PowerPC64".to_string(),
            goblin::elf::header::EM_S390 => "s390x".to_string(),
            goblin::elf::header::EM_RISCV => "RISC-V".to_string(),
            _ => format!("Unknown ({})", elf.header.e_machine),
        };
        
        let bit_width = match elf.header.e_ident[goblin::elf::header::EI_CLASS] {
            goblin::elf::header::ELFCLASS32 => 32,
            goblin::elf::header::ELFCLASS64 => 64,
            _ => 0,
        };
        
        let endian = match elf.header.e_ident[goblin::elf::header::EI_DATA] {
            goblin::elf::header::ELFDATA2LSB => "Little Endian".to_string(),
            goblin::elf::header::ELFDATA2MSB => "Big Endian".to_string(),
            _ => "Unknown".to_string(),
        };
        
        let elf_type = match elf.header.e_type {
            goblin::elf::header::ET_NONE => "None".to_string(),
            goblin::elf::header::ET_REL => "Relocatable".to_string(),
            goblin::elf::header::ET_EXEC => "Executable".to_string(),
            goblin::elf::header::ET_DYN => "Shared Object".to_string(),
            goblin::elf::header::ET_CORE => "Core".to_string(),
            _ => format!("Unknown ({})", elf.header.e_type),
        };
        
        let machine = match elf.header.e_machine {
            goblin::elf::header::EM_386 => "Intel 80386".to_string(),
            goblin::elf::header::EM_X86_64 => "AMD x86-64".to_string(),
            goblin::elf::header::EM_ARM => "ARM".to_string(),
            goblin::elf::header::EM_AARCH64 => "AArch64".to_string(),
            goblin::elf::header::EM_MIPS => "MIPS".to_string(),
            goblin::elf::header::EM_PPC => "PowerPC".to_string(),
            goblin::elf::header::EM_PPC64 => "PowerPC64".to_string(),
            goblin::elf::header::EM_S390 => "IBM S/390".to_string(),
            goblin::elf::header::EM_RISCV => "RISC-V".to_string(),
            _ => "Unknown".to_string(),
        };
        
        let file_name = Path::new(path)
            .file_name()
            .unwrap_or_default()
            .to_string_lossy()
            .to_string();
        
        Ok(ElfInfo {
            file_name,
            file_path: path.to_string(),
            file_size: metadata.len(),
            architecture,
            bit_width,
            endian,
            elf_type,
            machine,
            entry_point: elf.entry,
            is_valid: true,
            is_shared_library: elf.header.e_type == goblin::elf::header::ET_DYN,
        })
    }

    pub fn get_sections(&self) -> Result<Vec<(String, u64, u64)>> {
        let elf = self.elf.as_ref()
            .context("ELF not loaded")?;
        
        let sections = elf.section_headers.iter()
            .map(|sh| {
                let name = elf.shdr_strtab.get_at(sh.sh_name)
                    .unwrap_or("")
                    .to_string();
                (name, sh.sh_addr, sh.sh_size)
            })
            .collect();
        
        Ok(sections)
    }

    pub fn get_section_by_name(&self, name: &str) -> Result<Option<(u64, u64, u64)>> {
        let elf = self.elf.as_ref()
            .context("ELF not loaded")?;
        
        for sh in &elf.section_headers {
            if let Some(section_name) = elf.shdr_strtab.get_at(sh.sh_name) {
                if section_name == name {
                    return Ok(Some((sh.sh_addr, sh.sh_offset, sh.sh_size)));
                }
            }
        }
        
        Ok(None)
    }

    pub fn has_symtab(&self) -> bool {
        self.get_section_by_name(".symtab")
            .map(|s| s.is_some())
            .unwrap_or(false)
    }

    pub fn has_dynsym(&self) -> bool {
        self.get_section_by_name(".dynsym")
            .map(|s| s.is_some())
            .unwrap_or(false)
    }

    pub fn is_elf_file(path: &str) -> bool {
        if let Ok(data) = fs::read(path) {
            if data.len() >= 4 {
                return data[0] == 0x7f 
                    && data[1] == b'E' 
                    && data[2] == b'L' 
                    && data[3] == b'F';
            }
        }
        false
    }

    pub fn get_elf(&self) -> Option<&Elf> {
        self.elf.as_ref()
    }

    pub fn get_data(&self) -> &[u8] {
        &self.data
    }
}
