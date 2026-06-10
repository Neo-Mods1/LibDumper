package com.neomods.libdumper.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neomods.libdumper.domain.DumpConfig
import com.neomods.libdumper.domain.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DUMP_LOCATION = stringPreferencesKey("dump_location")
        private val EXTRACT_SYMTAB = booleanPreferencesKey("extract_symtab")
        private val EXTRACT_DYNSYM = booleanPreferencesKey("extract_dynsym")
        private val EXTRACT_EXPORTED = booleanPreferencesKey("extract_exported")
        private val EXTRACT_IMPORTED = booleanPreferencesKey("extract_imported")
        private val DUMP_RAW_NAMES = booleanPreferencesKey("dump_raw_names")
        private val GENERATE_CPP = booleanPreferencesKey("generate_cpp")
        private val GROUP_METHODS = booleanPreferencesKey("group_methods")
        private val GROUP_STATIC = booleanPreferencesKey("group_static")
        private val DETECT_CONSTRUCTORS = booleanPreferencesKey("detect_constructors")
        private val DETECT_DESTRUCTORS = booleanPreferencesKey("detect_destructors")
        private val DETECT_OVERLOADED = booleanPreferencesKey("detect_overloaded")
        private val DETECT_NAMESPACES = booleanPreferencesKey("detect_namespaces")
        private val GENERATE_COMMENTS = booleanPreferencesKey("generate_comments")
        private val INCLUDE_RVA = booleanPreferencesKey("include_rva")
        private val INCLUDE_FILE_OFFSETS = booleanPreferencesKey("include_file_offsets")
        private val INCLUDE_SYMBOL_SIZES = booleanPreferencesKey("include_symbol_sizes")
        private val INCLUDE_SECTION_NAMES = booleanPreferencesKey("include_section_names")
        private val GENERATE_DUMP_CPP = booleanPreferencesKey("generate_dump_cpp")
        private val GENERATE_SYMBOL_TABLE = booleanPreferencesKey("generate_symbol_table")
        private val GENERATE_CREDITS = booleanPreferencesKey("generate_credits")
        private val GENERATE_DUMP_INFO = booleanPreferencesKey("generate_dump_info")
        private val GENERATE_JSON = booleanPreferencesKey("generate_json")
        private val RECENT_LIBRARIES = stringPreferencesKey("recent_libraries")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        when (preferences[THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val dumpLocation: Flow<String> = dataStore.data.map { preferences ->
        preferences[DUMP_LOCATION] ?: "/storage/emulated/0/Dumper"
    }

    val dumpConfig: Flow<DumpConfig> = dataStore.data.map { preferences ->
        DumpConfig(
            extractSymtab = preferences[EXTRACT_SYMTAB] ?: true,
            extractDynsym = preferences[EXTRACT_DYNSYM] ?: true,
            extractExported = preferences[EXTRACT_EXPORTED] ?: true,
            extractImported = preferences[EXTRACT_IMPORTED] ?: true,
            dumpRawNames = preferences[DUMP_RAW_NAMES] ?: true,
            generateCppReconstruction = preferences[GENERATE_CPP] ?: true,
            groupMethodsIntoClasses = preferences[GROUP_METHODS] ?: true,
            groupStaticMethods = preferences[GROUP_STATIC] ?: true,
            detectConstructors = preferences[DETECT_CONSTRUCTORS] ?: true,
            detectDestructors = preferences[DETECT_DESTRUCTORS] ?: true,
            detectOverloadedMethods = preferences[DETECT_OVERLOADED] ?: true,
            detectNamespaces = preferences[DETECT_NAMESPACES] ?: true,
            generateComments = preferences[GENERATE_COMMENTS] ?: true,
            includeRva = preferences[INCLUDE_RVA] ?: true,
            includeFileOffsets = preferences[INCLUDE_FILE_OFFSETS] ?: true,
            includeSymbolSizes = preferences[INCLUDE_SYMBOL_SIZES] ?: true,
            includeSectionNames = preferences[INCLUDE_SECTION_NAMES] ?: true,
            generateDumpCpp = preferences[GENERATE_DUMP_CPP] ?: true,
            generateSymbolTable = preferences[GENERATE_SYMBOL_TABLE] ?: true,
            generateCredits = preferences[GENERATE_CREDITS] ?: true,
            generateDumpInfo = preferences[GENERATE_DUMP_INFO] ?: true,
            generateJson = preferences[GENERATE_JSON] ?: true
        )
    }

    val recentLibraries: Flow<List<String>> = dataStore.data.map { preferences ->
        val json = preferences[RECENT_LIBRARIES] ?: "[]"
        try {
            com.google.gson.Gson().fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setDumpLocation(location: String) {
        dataStore.edit { preferences ->
            preferences[DUMP_LOCATION] = location
        }
    }

    suspend fun updateDumpConfig(config: DumpConfig) {
        dataStore.edit { preferences ->
            preferences[EXTRACT_SYMTAB] = config.extractSymtab
            preferences[EXTRACT_DYNSYM] = config.extractDynsym
            preferences[EXTRACT_EXPORTED] = config.extractExported
            preferences[EXTRACT_IMPORTED] = config.extractImported
            preferences[DUMP_RAW_NAMES] = config.dumpRawNames
            preferences[GENERATE_CPP] = config.generateCppReconstruction
            preferences[GROUP_METHODS] = config.groupMethodsIntoClasses
            preferences[GROUP_STATIC] = config.groupStaticMethods
            preferences[DETECT_CONSTRUCTORS] = config.detectConstructors
            preferences[DETECT_DESTRUCTORS] = config.detectDestructors
            preferences[DETECT_OVERLOADED] = config.detectOverloadedMethods
            preferences[DETECT_NAMESPACES] = config.detectNamespaces
            preferences[GENERATE_COMMENTS] = config.generateComments
            preferences[INCLUDE_RVA] = config.includeRva
            preferences[INCLUDE_FILE_OFFSETS] = config.includeFileOffsets
            preferences[INCLUDE_SYMBOL_SIZES] = config.includeSymbolSizes
            preferences[INCLUDE_SECTION_NAMES] = config.includeSectionNames
            preferences[GENERATE_DUMP_CPP] = config.generateDumpCpp
            preferences[GENERATE_SYMBOL_TABLE] = config.generateSymbolTable
            preferences[GENERATE_CREDITS] = config.generateCredits
            preferences[GENERATE_DUMP_INFO] = config.generateDumpInfo
            preferences[GENERATE_JSON] = config.generateJson
        }
    }

    suspend fun addRecentLibrary(path: String) {
        dataStore.edit { preferences ->
            val currentList = try {
                val json = preferences[RECENT_LIBRARIES] ?: "[]"
                com.google.gson.Gson().fromJson(json, Array<String>::class.java)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            currentList.remove(path)
            currentList.add(0, path)

            if (currentList.size > 10) {
                currentList.removeLast()
            }

            preferences[RECENT_LIBRARIES] = com.google.gson.Gson().toJson(currentList)
        }
    }

    suspend fun clearRecentLibraries() {
        dataStore.edit { preferences ->
            preferences[RECENT_LIBRARIES] = "[]"
        }
    }
}
