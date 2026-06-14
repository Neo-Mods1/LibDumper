package com.neomods.libdumper.jni

import com.neomods.libdumper.domain.DumpConfig
import com.neomods.libdumper.domain.ElfInfo
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.StringReader

class NativeLibWrapper {

    companion object {
        private var nativeLoaded = false

        init {
            try {
                System.loadLibrary("NeoLibDumper")
                nativeLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
                nativeLoaded = false
            }
        }

        private val gson = Gson()

        private var lastSymbolsJson: String? = null
        private var lastClassesJson: String? = null
        private var lastNamespacesJson: String? = null

        fun isNativeAvailable(): Boolean = nativeLoaded

        fun loadElf(path: String): ElfInfo? {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val json = nativeLoadElf(path)
                gson.fromJson(json, ElfInfo::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun validateElf(path: String): Boolean {
            if (!nativeLoaded) return false
            return try {
                nativeValidateElf(path)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun extractSymbols(path: String, config: DumpConfig): Int {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val json = nativeExtractSymbols(
                    path,
                    config.extractSymtab,
                    config.extractDynsym,
                    config.extractExported,
                    config.extractImported,
                    config.dumpRawNames,
                    config.generateCppReconstruction,
                    config.groupMethodsIntoClasses,
                    config.groupStaticMethods,
                    config.detectConstructors,
                    config.detectDestructors,
                    config.detectOverloadedMethods,
                    config.detectNamespaces,
                    config.generateComments,
                    config.includeMethodSignatures,
                    config.includeReturnTypes,
                    config.includeParameterTypes,
                    config.attemptInheritanceDetection,
                    config.includeVirtualAddresses,
                    config.includeRva,
                    config.includeFileOffsets,
                    config.includeSymbolSizes,
                    config.includeSectionNames,
                    config.generateDumpCpp,
                    config.generateSymbolTable,
                    config.generateCredits,
                    config.generateDumpInfo,
                    config.generateJson
                )
                lastSymbolsJson = json
                lastClassesJson = null
                lastNamespacesJson = null
                countJsonArrayItems(json)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }

        fun reconstructClasses(config: DumpConfig): Int {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val symbolsJson = lastSymbolsJson ?: return 0
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
                lastClassesJson = json
                countJsonArrayItems(json)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }

        fun detectNamespaces(config: DumpConfig): Int {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val symbolsJson = lastSymbolsJson ?: return 0
                val classesJson = lastClassesJson ?: return 0
                val json = nativeDetectNamespaces(
                    symbolsJson,
                    classesJson,
                    config.detectNamespaces
                )
                lastNamespacesJson = json
                countJsonArrayItems(json)
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }

        fun generateDumpCpp(config: DumpConfig): String {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val classesJson = lastClassesJson ?: return "// No class data"
                val namespacesJson = lastNamespacesJson ?: "[]"
                nativeGenerateDumpCpp(
                    classesJson,
                    namespacesJson,
                    config.generateComments,
                    config.includeMethodSignatures,
                    config.includeVirtualAddresses,
                    config.includeRva,
                    config.includeFileOffsets,
                    config.includeSymbolSizes,
                    config.includeSectionNames,
                    config.detectNamespaces
                )
            } catch (e: Exception) {
                e.printStackTrace()
                "// Error generating Dump.cpp: ${e.message}"
            }
        }

        fun generateSymbolTable(config: DumpConfig): String {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val symbolsJson = lastSymbolsJson ?: return "// No symbol data"
                nativeGenerateSymbolTable(
                    symbolsJson,
                    config.includeVirtualAddresses,
                    config.includeRva,
                    config.includeFileOffsets,
                    config.includeSymbolSizes,
                    config.includeSectionNames
                )
            } catch (e: Exception) {
                e.printStackTrace()
                "// Error generating SymbolTable.txt: ${e.message}"
            }
        }

        fun generateJsonExport(elfInfo: ElfInfo): String {
            if (!nativeLoaded) throw IllegalStateException("Native library not loaded")
            return try {
                val elfInfoJson = gson.toJson(elfInfo)
                val symbolsJson = lastSymbolsJson ?: "[]"
                val classesJson = lastClassesJson ?: "[]"
                val namespacesJson = lastNamespacesJson ?: "[]"
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
            if (!nativeLoaded) return "1.0.0 (native not loaded)"
            return try {
                nativeGetVersion()
            } catch (e: Exception) {
                "1.0.0"
            }
        }

        private fun countJsonArrayItems(json: String): Int {
            return try {
                val reader = JsonReader(StringReader(json))
                var count = 0
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.skipValue()
                    count++
                }
                reader.endArray()
                count
            } catch (e: Exception) {
                0
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
            dumpRawNames: Boolean,
            generateCppReconstruction: Boolean,
            groupMethodsIntoClasses: Boolean,
            groupStaticMethods: Boolean,
            detectConstructors: Boolean,
            detectDestructors: Boolean,
            detectOverloadedMethods: Boolean,
            detectNamespaces: Boolean,
            generateComments: Boolean,
            includeMethodSignatures: Boolean,
            includeReturnTypes: Boolean,
            includeParameterTypes: Boolean,
            attemptInheritanceDetection: Boolean,
            includeVirtualAddresses: Boolean,
            includeRva: Boolean,
            includeFileOffsets: Boolean,
            includeSymbolSizes: Boolean,
            includeSectionNames: Boolean,
            generateDumpCpp: Boolean,
            generateSymbolTable: Boolean,
            generateCredits: Boolean,
            generateDumpInfo: Boolean,
            generateJson: Boolean
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
            includeMethodSignatures: Boolean,
            includeVirtualAddresses: Boolean,
            includeRva: Boolean,
            includeFileOffsets: Boolean,
            includeSymbolSizes: Boolean,
            includeSectionNames: Boolean,
            detectNamespaces: Boolean
        ): String

        private external fun nativeGenerateSymbolTable(
            symbolsJson: String,
            includeVirtualAddresses: Boolean,
            includeRva: Boolean,
            includeFileOffsets: Boolean,
            includeSymbolSizes: Boolean,
            includeSectionNames: Boolean
        ): String

        private external fun nativeGenerateJsonExport(
            elfInfoJson: String,
            symbolsJson: String,
            classesJson: String,
            namespacesJson: String
        ): String

        private external fun nativeGetVersion(): String
    }
}
