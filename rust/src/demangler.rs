use cpp_demangle::{DemangleOptions, Symbol as CppSymbol};

pub struct Demangler {
    options: DemangleOptions,
}

impl Demangler {
    pub fn new() -> Self {
        Self {
            options: DemangleOptions::default()
                .show_return_type(true)
                .show_template_parameters(true)
                .show_function_parameters(true)
                .hide_ellipsis(true),
        }
    }

    pub fn demangle(&self, mangled: &str) -> String {
        if mangled.is_empty() {
            return mangled.to_string();
        }
        
        if !self.is_mangled(mangled) {
            return mangled.to_string();
        }
        
        match CppSymbol::new(mangled) {
            Ok(symbol) => {
                match symbol.demangle(&self.options) {
                    Ok(demangled) => demangled,
                    Err(_) => mangled.to_string(),
                }
            }
            Err(_) => mangled.to_string(),
        }
    }

    pub fn demangle_batch(&self, mangled_names: &[String]) -> Vec<String> {
        mangled_names.iter()
            .map(|name| self.demangle(name))
            .collect()
    }

    fn is_mangled(&self, name: &str) -> bool {
        if name.starts_with("_Z") {
            return true;
        }
        
        if name.starts_with("__Z") {
            return true;
        }
        
        if name.contains("@") && name.starts_with("_") {
            return true;
        }
        
        if name.starts_with("_ZN") || name.starts_with("_ZL") {
            return true;
        }
        
        false
    }

    pub fn extract_class_name(&self, mangled: &str) -> Option<String> {
        let demangled = self.demangle(mangled);
        
        if let Some(colon_pos) = demangled.find("::") {
            let parts: Vec<&str> = demangled[..colon_pos].split("::").collect();
            if let Some(class_name) = parts.last() {
                return Some(class_name.to_string());
            }
        }
        
        None
    }

    pub fn extract_namespace(&self, mangled: &str) -> Option<String> {
        let demangled = self.demangle(mangled);
        
        let parts: Vec<&str> = demangled.split("::").collect();
        if parts.len() > 1 {
            let namespace_parts = &parts[..parts.len() - 1];
            if !namespace_parts.is_empty() {
                return Some(namespace_parts.join("::"));
            }
        }
        
        None
    }

    pub fn extract_method_name(&self, mangled: &str) -> Option<String> {
        let demangled = self.demangle(mangled);
        
        if let Some(last_colon) = demangled.rfind("::") {
            let method_part = &demangled[last_colon + 2..];
            if let Some(paren_pos) = method_part.find('(') {
                return Some(method_part[..paren_pos].to_string());
            }
            return Some(method_part.to_string());
        }
        
        None
    }

    pub fn is_constructor(&self, mangled: &str, class_name: &str) -> bool {
        let demangled = self.demangle(mangled);
        let constructor_pattern = format!("{}::{}", class_name, class_name);
        demangled.contains(&constructor_pattern)
    }

    pub fn is_destructor(&self, mangled: &str, class_name: &str) -> bool {
        let demangled = self.demangle(mangled);
        let destructor_pattern = format!("{}::~{}", class_name, class_name);
        demangled.contains(&destructor_pattern)
    }

    pub fn is_static_method(&self, mangled: &str) -> bool {
        let demangled = self.demangle(mangled);
        demangled.contains("static") || mangled.contains("_ZL")
    }

    pub fn extract_return_type(&self, mangled: &str) -> Option<String> {
        let demangled = self.demangle(mangled);
        
        if let Some(paren_pos) = demangled.find('(') {
            let before_paren = demangled[..paren_pos].trim();
            let parts: Vec<&str> = before_paren.split("::").collect();
            if parts.len() > 1 {
                return Some(parts[..parts.len() - 1].join("::"));
            }
        }
        
        None
    }

    pub fn extract_parameters(&self, mangled: &str) -> Vec<String> {
        let demangled = self.demangle(mangled);
        
        if let Some(start) = demangled.find('(') {
            if let Some(end) = demangled.rfind(')') {
                let params_str = &demangled[start + 1..end];
                if params_str.is_empty() {
                    return Vec::new();
                }
                
                return params_str.split(',')
                    .map(|p| p.trim().to_string())
                    .filter(|p| !p.is_empty())
                    .collect();
            }
        }
        
        Vec::new()
    }
}
