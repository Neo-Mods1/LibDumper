package com.neomods.libdumper.jni

import com.neomods.libdumper.domain.ClassInfo
import com.neomods.libdumper.domain.DumpConfig
import com.neomods.libdumper.domain.ElfInfo
import com.neomods.libdumper.domain.MethodInfo
import com.neomods.libdumper.domain.NamespaceInfo
import com.neomods.libdumper.domain.Symbol
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NativeLibWrapper {

    companion object {
        init {
            try {
                System.loadLibrary("NeoLibDumper")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }

        private val gson = Gson()

        fun loadElf(path: String): ElfInfo? {
            return try {
                val json = nativeLoadElf(path)
                gson.fromJson(json, ElfInfo::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun validateElf(path: String): Boolean {
            return try {
                nativeValidateElf(path)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun extractSymbols(
            path: String,
            config: DumpConfig
        ): List<Symbol> {
            return try {
                val json = nativeExtractSymbols(
                    path,
                    config.extractSymtab,
                    config.extractDynsym,
                    config.extractExported,
                    config.extractImported,
                    config.dumpRawNames
                )
                val type = object : TypeToken<List<Symbol>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        fun reconstructClasses(
            symbols: List<Symbol>,
            config: DumpConfig
        ): List<ClassInfo> {
            return try {
                val symbolsJson = gson.toJson(symbols)
                val json = nativeReconstructClasses(
                    symbolsJson,
                    config.generateCppReconstruction,
                    config.groupMethodsIntoClasses,
                    config.groupStaticMethods,
                    config.detectConstructors,
                    config.detectDestructors,
                    config.detectOverloadedMethods,
                    config.attemptInheritanceDetection
                )
                val type = object : TypeToken<List<ClassInfo>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        fun detectNamespaces(
            symbols: List<Symbol>,
            classes: List<ClassInfo>,
            config: DumpConfig
        ): List<NamespaceInfo> {
            return try {
                val symbolsJson = gson.toJson(symbols)
                val classesJson = gson.toJson(classes)
                val json = nativeDetectNamespaces(
                    symbolsJson,
                    classesJson,
                    config.detectNamespaces
                )
                val type = object : TypeToken<List<NamespaceInfo>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

        fun generateDumpCpp(
            classes: List<ClassInfo>,
            namespaces: List<NamespaceInfo>,
            config: DumpConfig
        ): String {
            return try {
                val classesJson = gson.toJson(classes)
                val namespacesJson = gson.toJson(namespaces)
                nativeGenerateDumpCpp(
                    classesJson,
                    namespacesJson,
                    config.generateComments,
                    config.includeRva,
                    config.includeFileOffsets,
                    config.includeSymbolSizes,
                    config.includeSectionNames
                )
            } catch (e: Exception) {
                e.printStackTrace()
                "// Error generating Dump.cpp: ${e.message}"
            }
        }

        fun generateSymbolTable(symbols: List<Symbol>): String {
            return try {
                val symbolsJson = gson.toJson(symbols)
                nativeGenerateSymbolTable(symbolsJson)
            } catch (e: Exception) {
                e.printStackTrace()
                "// Error generating SymbolTable.txt: ${e.message}"
            }
        }

        fun generateJsonExport(
            elfInfo: ElfInfo,
            symbols: List<Symbol>,
            classes: List<ClassInfo>,
            namespaces: List<NamespaceInfo>
        ): String {
            return try {
                val elfInfoJson = gson.toJson(elfInfo)
                val symbolsJson = gson.toJson(symbols)
                val classesJson = gson.toJson(classes)
                val namespacesJson = gson.toJson(namespaces)
                nativeGenerateJsonExport(
                    elfInfoJson,
                    symbolsJson,
                    classesJson,
                    namespacesJson
                )
            } catch (e: Exception) {
                e.printStackTrace()
                "// Error generating JSON export: ${e.message}"
            }
        }

        fun getVersion(): String {
            return try {
                nativeGetVersion()
            } catch (e: Exception) {
                "1.0.0"
            }
        }

        private external fun nativeLoadElf(path: String): String
        private external fun nativeValidateElf(path: String): Boolean
        private external fun nativeExtractSymbols(
            path: String,
            extractSymtab: Boolean,
            extractDynsym: Boolean,
            extractExported: Boolean,
            extractImported: Boolean,
            dumpRawNames: Boolean
        ): String

        private external fun nativeReconstructClasses(
            symbolsJson: String,
            generateCpp: Boolean,
            groupMethods: Boolean,
            groupStatic: Boolean,
            detectConstructors: Boolean,
            detectDestructors: Boolean,
            detectOverloaded: Boolean,
            attemptInheritance: Boolean
        ): String

        private external fun nativeDetectNamespaces(
            symbolsJson: String,
            classesJson: String,
            detectNamespaces: Boolean
        ): String

        private external fun nativeGenerateDumpCpp(
            classesJson: String,
            namespacesJson: String,
            generateComments: Boolean,
            includeRva: Boolean,
            includeFileOffsets: Boolean,
            includeSymbolSizes: Boolean,
            includeSectionNames: Boolean
        ): String

        private external fun nativeGenerateSymbolTable(symbolsJson: String): String

        private external fun nativeGenerateJsonExport(
            elfInfoJson: String,
            symbolsJson: String,
            classesJson: String,
            namespacesJson: String
        ): String

        private external fun nativeGetVersion(): String
    }
}
