package com.neomods.libdumper.domain

data class ElfInfo(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val architecture: String,
    val bitWidth: Int,
    val endian: String,
    val elfType: String,
    val machine: String,
    val entryPoint: Long,
    val isValid: Boolean,
    val isSharedLibrary: Boolean
)

data class Symbol(
    val name: String,
    val demangledName: String,
    val address: Long,
    val rva: Long,
    val fileOffset: Long,
    val size: Long,
    val section: String,
    val sectionIndex: Int,
    val symbolType: String,
    val binding: String,
    val isExported: Boolean,
    val isImported: Boolean,
    val isFunction: Boolean,
    val isObject: Boolean
)

data class ClassInfo(
    val name: String,
    val namespace: String,
    val fullPath: String,
    val methods: List<MethodInfo>,
    val constructors: List<MethodInfo>,
    val destructors: List<MethodInfo>,
    val staticMethods: List<MethodInfo>,
    val overloadedMethods: List<List<MethodInfo>>,
    val baseClass: String?,
    val derivedClasses: List<String>
)

data class MethodInfo(
    val name: String,
    val demangledName: String,
    val address: Long,
    val rva: Long,
    val fileOffset: Long,
    val size: Long,
    val section: String,
    val returnType: String?,
    val parameters: List<String>,
    val isStatic: Boolean,
    val isVirtual: Boolean,
    val isConst: Boolean,
    val isOverloaded: Boolean,
    val overloadIndex: Int
)

data class NamespaceInfo(
    val name: String,
    val fullPath: String,
    val parent: String?,
    val subNamespaces: List<String>,
    val classes: List<String>,
    val functions: List<String>
)

data class DumpConfig(
    val extractSymtab: Boolean = true,
    val extractDynsym: Boolean = true,
    val extractExported: Boolean = true,
    val extractImported: Boolean = true,
    val dumpRawNames: Boolean = true,
    val generateCppReconstruction: Boolean = true,
    val groupMethodsIntoClasses: Boolean = true,
    val groupStaticMethods: Boolean = true,
    val detectConstructors: Boolean = true,
    val detectDestructors: Boolean = true,
    val detectOverloadedMethods: Boolean = true,
    val detectNamespaces: Boolean = true,
    val generateComments: Boolean = true,
    val includeMethodSignatures: Boolean = true,
    val includeReturnTypes: Boolean = true,
    val includeParameterTypes: Boolean = true,
    val attemptInheritanceDetection: Boolean = true,
    val includeVirtualAddresses: Boolean = true,
    val includeRva: Boolean = true,
    val includeFileOffsets: Boolean = true,
    val includeSymbolSizes: Boolean = true,
    val includeSectionNames: Boolean = true,
    val generateDumpCpp: Boolean = true,
    val generateSymbolTable: Boolean = true,
    val generateCredits: Boolean = true,
    val generateDumpInfo: Boolean = true,
    val generateJson: Boolean = true
)

data class DumpResult(
    val elfInfo: ElfInfo,
    val symbols: List<Symbol>,
    val classes: List<ClassInfo>,
    val namespaces: List<NamespaceInfo>,
    val dumpCpp: String?,
    val symbolTable: String?,
    val dumpInfo: String?,
    val credits: String?,
    val jsonExport: String?,
    val totalSymbols: Int,
    val totalClasses: Int,
    val totalMethods: Int,
    val totalNamespaces: Int,
    val dumpDurationMs: Long
)

data class DumpProgress(
    val stage: String,
    val progress: Float,
    val message: String
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
