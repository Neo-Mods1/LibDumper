package com.neomods.libdumper.domain

import com.google.gson.annotations.SerializedName

data class ElfInfo(
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("file_size") val fileSize: Long,
    val architecture: String,
    @SerializedName("bit_width") val bitWidth: Int,
    val endian: String,
    @SerializedName("elf_type") val elfType: String,
    val machine: String,
    @SerializedName("entry_point") val entryPoint: Long,
    @SerializedName("is_valid") val isValid: Boolean,
    @SerializedName("is_shared_library") val isSharedLibrary: Boolean
)

data class Symbol(
    val name: String,
    @SerializedName("demangled_name") val demangledName: String,
    val address: Long,
    val rva: Long,
    @SerializedName("file_offset") val fileOffset: Long,
    val size: Long,
    val section: String,
    @SerializedName("section_index") val sectionIndex: Int,
    @SerializedName("symbol_type") val symbolType: String,
    val binding: String,
    @SerializedName("is_exported") val isExported: Boolean,
    @SerializedName("is_imported") val isImported: Boolean,
    @SerializedName("is_function") val isFunction: Boolean,
    @SerializedName("is_object") val isObject: Boolean
)

data class ClassInfo(
    val name: String,
    val namespace: String,
    @SerializedName("full_path") val fullPath: String,
    val methods: List<MethodInfo>,
    val constructors: List<MethodInfo>,
    val destructors: List<MethodInfo>,
    @SerializedName("static_methods") val staticMethods: List<MethodInfo>,
    @SerializedName("overloaded_methods") val overloadedMethods: List<List<MethodInfo>>,
    @SerializedName("base_class") val baseClass: String?,
    @SerializedName("derived_classes") val derivedClasses: List<String>
)

data class MethodInfo(
    val name: String,
    @SerializedName("demangled_name") val demangledName: String,
    val address: Long,
    val rva: Long,
    @SerializedName("file_offset") val fileOffset: Long,
    val size: Long,
    val section: String,
    @SerializedName("return_type") val returnType: String?,
    val parameters: List<String>,
    @SerializedName("is_static") val isStatic: Boolean,
    @SerializedName("is_virtual") val isVirtual: Boolean,
    @SerializedName("is_const") val isConst: Boolean,
    @SerializedName("is_overloaded") val isOverloaded: Boolean,
    @SerializedName("overload_index") val overloadIndex: Int
)

data class NamespaceInfo(
    val name: String,
    @SerializedName("full_path") val fullPath: String,
    val parent: String?,
    @SerializedName("sub_namespaces") val subNamespaces: List<String>,
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
