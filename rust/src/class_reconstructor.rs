use std::collections::HashMap;
use anyhow::Result;
use rayon::prelude::*;

use crate::types::{Symbol, ClassInfo, MethodInfo, DumpConfig};
use crate::demangler::Demangler;

pub struct ClassReconstructor {
    demangler: Demangler,
}

impl ClassReconstructor {
    pub fn new() -> Self {
        Self {
            demangler: Demangler::new(),
        }
    }

    pub fn reconstruct_classes(
        &self,
        symbols: &[Symbol],
        config: &DumpConfig,
    ) -> Result<Vec<ClassInfo>> {
        let mut class_map: HashMap<String, Vec<Symbol>> = HashMap::new();
        
        let valid_symbols: Vec<&Symbol> = symbols.iter()
            .filter(|s| {
                s.is_function && 
                !s.name.starts_with("_ZL") &&
                self.demangler.extract_class_name(&s.name).is_some()
            })
            .collect();
        
        for symbol in valid_symbols {
            if let Some(class_name) = self.demangler.extract_class_name(&symbol.name) {
                class_map.entry(class_name)
                    .or_insert_with(Vec::new)
                    .push(symbol.clone());
            }
        }
        
        let mut classes: Vec<ClassInfo> = class_map.into_par_iter()
            .map(|(class_name, class_symbols)| {
                self.build_class_info(&class_name, &class_symbols, config)
            })
            .collect();
        
        classes.sort_by(|a, b| a.name.cmp(&b.name));
        
        if config.attempt_inheritance_detection {
            self.detect_inheritance(&mut classes);
        }
        
        Ok(classes)
    }

    fn build_class_info(
        &self,
        class_name: &str,
        symbols: &[Symbol],
        config: &DumpConfig,
    ) -> ClassInfo {
        let mut methods = Vec::new();
        let mut constructors = Vec::new();
        let mut destructors = Vec::new();
        let mut static_methods = Vec::new();
        
        for symbol in symbols {
            let method_info = self.build_method_info(symbol, class_name, config);
            
            if config.detect_constructors && self.demangler.is_constructor(&symbol.name, class_name) {
                constructors.push(method_info.clone());
            } else if config.detect_destructors && self.demangler.is_destructor(&symbol.name, class_name) {
                destructors.push(method_info.clone());
            } else if config.group_static_methods && self.demangler.is_static_method(&symbol.name) {
                static_methods.push(method_info.clone());
            } else {
                methods.push(method_info);
            }
        }
        
        let overloaded_methods = if config.detect_overloaded_methods {
            self.group_overloaded_methods(&methods)
        } else {
            Vec::new()
        };
        
        let namespace = self.demangler.extract_namespace(
            symbols.first().map(|s| s.name.as_str()).unwrap_or("")
        ).unwrap_or_default();
        
        ClassInfo {
            name: class_name.to_string(),
            namespace: namespace.clone(),
            full_path: if namespace.is_empty() {
                class_name.to_string()
            } else {
                format!("{}::{}", namespace, class_name)
            },
            methods,
            constructors,
            destructors,
            static_methods,
            overloaded_methods,
            base_class: None,
            derived_classes: Vec::new(),
        }
    }

    fn build_method_info(&self, symbol: &Symbol, class_name: &str, config: &DumpConfig) -> MethodInfo {
        let demangled = &symbol.demangled_name;
        
        let return_type = if config.include_return_types {
            self.demangler.extract_return_type(&symbol.name)
        } else {
            None
        };
        
        let parameters = if config.include_parameter_types {
            self.demangler.extract_parameters(&symbol.name)
        } else {
            Vec::new()
        };
        
        let is_static = self.demangler.is_static_method(&symbol.name);
        
        let method_name = self.demangler.extract_method_name(&symbol.name)
            .unwrap_or_else(|| symbol.name.clone());
        
        MethodInfo {
            name: method_name,
            demangled_name: demangled.clone(),
            address: symbol.address,
            rva: symbol.rva,
            file_offset: symbol.file_offset,
            size: symbol.size,
            section: symbol.section.clone(),
            return_type,
            parameters,
            is_static,
            is_virtual: false,
            is_const: demangled.contains("const"),
            is_overloaded: false,
            overload_index: 0,
        }
    }

    fn group_overloaded_methods(&self, methods: &[MethodInfo]) -> Vec<Vec<MethodInfo>> {
        let mut method_groups: HashMap<String, Vec<MethodInfo>> = HashMap::new();
        
        for method in methods {
            let key = format!("{}::{}", 
                method.name, 
                method.parameters.join(",")
            );
            method_groups.entry(key)
                .or_insert_with(Vec::new)
                .push(method.clone());
        }
        
        method_groups.into_values()
            .filter(|group| group.len() > 1)
            .collect()
    }

    fn detect_inheritance(&self, classes: &mut Vec<ClassInfo>) {
        let class_names: Vec<String> = classes.iter().map(|c| c.name.clone()).collect();
        
        for i in 0..classes.len() {
            for j in 0..classes.len() {
                if i == j {
                    continue;
                }
                
                let parent_name = &classes[j].name;
                let child_name = &classes[i].name;
                
                if self.could_be_derived(child_name, parent_name) {
                    classes[i].base_class = Some(parent_name.clone());
                    classes[j].derived_classes.push(child_name.clone());
                }
            }
        }
    }

    fn could_be_derived(&self, child: &str, parent: &str) -> bool {
        let demangled = self.demangler.demangle(child);
        
        if let Some(pos) = demangled.find("::") {
            let before = &demangled[..pos];
            if before == parent {
                return true;
            }
        }
        
        let inheritance_patterns = vec![
            format!("public {}", parent),
            format!("private {}", parent),
            format!("protected {}", parent),
        ];
        
        for pattern in inheritance_patterns {
            if demangled.contains(&pattern) {
                return true;
            }
        }
        
        false
    }

    pub fn generate_cpp_output(
        &self,
        classes: &[ClassInfo],
        namespaces: &[crate::types::NamespaceInfo],
        config: &DumpConfig,
    ) -> String {
        let mut output = String::new();
        
        output.push_str("// Lib Dumper - C++ Reconstruction Output\n");
        output.push_str("// Generated by Lib Dumper\n\n");
        
        let mut namespace_classes: HashMap<String, Vec<&ClassInfo>> = HashMap::new();
        
        for class in classes {
            let namespace = if class.namespace.is_empty() {
                "global".to_string()
            } else {
                class.namespace.clone()
            };
            namespace_classes.entry(namespace)
                .or_insert_with(Vec::new)
                .push(class);
        }
        
        for (namespace, namespace_class_list) in &namespace_classes {
            if namespace != "global" {
                output.push_str(&format!("namespace {}\n{{\n", namespace));
            }
            
            for class in namespace_class_list {
                output.push_str(&self.generate_class_cpp(class, config));
            }
            
            if namespace != "global" {
                output.push_str("}\n\n");
            }
        }
        
        for class in classes {
            if class.namespace.is_empty() {
                output.push_str(&self.generate_class_cpp(class, config));
            }
        }
        
        output
    }

    fn generate_class_cpp(&self, class: &ClassInfo, config: &DumpConfig) -> String {
        let mut output = String::new();
        
        if let Some(base) = &class.base_class {
            output.push_str(&format!("class {} : public {}\n{{\npublic:\n", class.name, base));
        } else {
            output.push_str(&format!("class {}\n{{\npublic:\n", class.name));
        }
        
        for constructor in &class.constructors {
            output.push_str(&format!("    {}();\n", class.name));
            if config.generate_comments {
                output.push_str(&self.generate_method_comment(constructor));
            }
            output.push('\n');
        }
        
        for destructor in &class.destructors {
            output.push_str(&format!("    ~{}();\n", class.name));
            if config.generate_comments {
                output.push_str(&self.generate_method_comment(destructor));
            }
            output.push('\n');
        }
        
        for method in &class.static_methods {
            output.push_str(&format!("    {};\n", method.demangled_name));
            if config.generate_comments {
                output.push_str(&self.generate_method_comment(method));
            }
            output.push('\n');
        }
        
        for method in &class.methods {
            output.push_str(&format!("    {};\n", method.demangled_name));
            if config.generate_comments {
                output.push_str(&self.generate_method_comment(method));
            }
            output.push('\n');
        }
        
        output.push_str("};\n\n");
        
        output
    }

    fn generate_method_comment(&self, method: &MethodInfo) -> String {
        let mut comment = String::from("    //");
        
        if method.rva != 0 {
            comment.push_str(&format!(" RVA: 0x{:X}", method.rva));
        }
        
        if method.address != 0 {
            comment.push_str(&format!(" | VA: 0x{:X}", method.address));
        }
        
        if method.file_offset != 0 {
            comment.push_str(&format!(" | Offset: 0x{:X}", method.file_offset));
        }
        
        if method.size != 0 {
            comment.push_str(&format!(" | Size: 0x{:X}", method.size));
        }
        
        if !method.section.is_empty() && method.section != "UNKNOWN" {
            comment.push_str(&format!(" | Section: {}", method.section));
        }
        
        comment.push('\n');
        comment
    }
}
