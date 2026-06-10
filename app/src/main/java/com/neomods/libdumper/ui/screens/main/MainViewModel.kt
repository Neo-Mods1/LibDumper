package com.neomods.libdumper.ui.screens.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.libdumper.domain.DumpConfig
import com.neomods.libdumper.domain.DumpProgress
import com.neomods.libdumper.domain.DumpResult
import com.neomods.libdumper.domain.ElfInfo
import com.neomods.libdumper.jni.NativeLibWrapper
import com.neomods.libdumper.storage.SettingsManager
import com.neomods.libdumper.utils.FileUtils
import com.neomods.libdumper.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MainUiState(
    val elfInfo: ElfInfo? = null,
    val isDumping: Boolean = false,
    val dumpProgress: DumpProgress? = null,
    val dumpResult: DumpResult? = null,
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _dumpConfig = MutableStateFlow(DumpConfig())
    val dumpConfig: StateFlow<DumpConfig> = _dumpConfig.asStateFlow()

    private val _recentLibraries = MutableStateFlow<List<String>>(emptyList())
    val recentLibraries: StateFlow<List<String>> = _recentLibraries.asStateFlow()

    private var currentUri: Uri? = null
    private var currentFilePath: String? = null

    init {
        viewModelScope.launch {
            settingsManager.dumpConfig.collect { config ->
                _dumpConfig.value = config
            }
        }
        viewModelScope.launch {
            settingsManager.recentLibraries.collect { libraries ->
                _recentLibraries.value = libraries
            }
        }
    }

    fun selectLibrary(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = FileUtils.getFileName(context, uri)
                val filePath = FileUtils.getFilePath(context, uri)
                
                if (!fileName.endsWith(".so")) {
                    _uiState.value = _uiState.value.copy(
                        error = "Selected file is not a .so library"
                    )
                    return@launch
                }

                val elfInfo = withContext(Dispatchers.IO) {
                    val path = if (filePath.isNotEmpty()) filePath else {
                        val tempFile = FileUtils.copyFileToInternal(context, uri, fileName)
                        tempFile.absolutePath
                    }
                    currentFilePath = path
                    NativeLibWrapper.loadElf(path)
                }

                if (elfInfo != null) {
                    _uiState.value = _uiState.value.copy(elfInfo = elfInfo)
                    currentUri = uri
                    settingsManager.addRecentLibrary(elfInfo.filePath)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load ELF file"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error selecting library: ${e.message}"
                )
            }
        }
    }

    fun selectLibraryByPath(context: Context, path: String) {
        viewModelScope.launch {
            try {
                val file = File(path)
                if (!file.exists()) {
                    _uiState.value = _uiState.value.copy(
                        error = "File not found: $path"
                    )
                    return@launch
                }

                if (!path.endsWith(".so")) {
                    _uiState.value = _uiState.value.copy(
                        error = "Selected file is not a .so library"
                    )
                    return@launch
                }

                val elfInfo = withContext(Dispatchers.IO) {
                    NativeLibWrapper.loadElf(path)
                }

                if (elfInfo != null) {
                    _uiState.value = _uiState.value.copy(elfInfo = elfInfo)
                    currentFilePath = path
                    settingsManager.addRecentLibrary(path)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load ELF file"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error loading library: ${e.message}"
                )
            }
        }
    }

    fun updateDumpConfig(config: DumpConfig) {
        _dumpConfig.value = config
        viewModelScope.launch {
            settingsManager.updateDumpConfig(config)
        }
    }

    fun startDump(context: Context) {
        val filePath = currentFilePath ?: run {
            _uiState.value = _uiState.value.copy(
                error = "No library selected"
            )
            return
        }

        val elfInfo = _uiState.value.elfInfo ?: run {
            _uiState.value = _uiState.value.copy(
                error = "No library selected"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDumping = true,
                dumpProgress = DumpProgress("Starting", 0f, "Initializing dump..."),
                dumpResult = null,
                error = null
            )

            try {
                val startTime = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Loading ELF", 0.1f, "Loading ELF file...")
                )
                notificationHelper.showDumpProgress("Loading ELF", 10)

                val symbols = withContext(Dispatchers.IO) {
                    NativeLibWrapper.extractSymbols(filePath, _dumpConfig.value)
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Reading Symbols", 0.3f, "Extracted ${symbols.size} symbols")
                )
                notificationHelper.showDumpProgress("Reading Symbols", 30)

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Detecting Classes", 0.5f, "Analyzing class structures...")
                )
                notificationHelper.showDumpProgress("Detecting Classes", 50)

                val classes = withContext(Dispatchers.IO) {
                    NativeLibWrapper.reconstructClasses(symbols, _dumpConfig.value)
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Detecting Namespaces", 0.6f, "Found ${classes.size} classes")
                )
                notificationHelper.showDumpProgress("Detecting Namespaces", 60)

                val namespaces = withContext(Dispatchers.IO) {
                    NativeLibWrapper.detectNamespaces(symbols, classes, _dumpConfig.value)
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Building Structures", 0.7f, "Found ${namespaces.size} namespaces")
                )
                notificationHelper.showDumpProgress("Building Structures", 70)

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Generating Files", 0.8f, "Generating output files...")
                )
                notificationHelper.showDumpProgress("Generating Files", 80)

                val dumpCpp = if (_dumpConfig.value.generateDumpCpp) {
                    withContext(Dispatchers.IO) {
                        NativeLibWrapper.generateDumpCpp(classes, namespaces, _dumpConfig.value)
                    }
                } else null

                val symbolTable = if (_dumpConfig.value.generateSymbolTable) {
                    withContext(Dispatchers.IO) {
                        NativeLibWrapper.generateSymbolTable(symbols)
                    }
                } else null

                val jsonExport = if (_dumpConfig.value.generateJson) {
                    withContext(Dispatchers.IO) {
                        NativeLibWrapper.generateJsonExport(elfInfo, symbols, classes, namespaces)
                    }
                } else null

                val dumpInfo = generateDumpInfo(elfInfo, symbols, classes, namespaces, startTime)
                val credits = generateCredits()

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Saving Output", 0.9f, "Saving files to disk...")
                )
                notificationHelper.showDumpProgress("Saving Output", 90)

                val dumpPath = withContext(Dispatchers.IO) {
                    saveDumpFiles(
                        context,
                        elfInfo,
                        dumpCpp,
                        symbolTable,
                        dumpInfo,
                        credits,
                        jsonExport,
                        symbols.size,
                        classes.size,
                        classes.sumOf { it.methods.size + it.constructors.size + it.destructors.size + it.staticMethods.size },
                        namespaces.size,
                        System.currentTimeMillis() - startTime
                    )
                }

                val result = DumpResult(
                    elfInfo = elfInfo,
                    symbols = symbols,
                    classes = classes,
                    namespaces = namespaces,
                    dumpCpp = dumpCpp,
                    symbolTable = symbolTable,
                    dumpInfo = dumpInfo,
                    credits = credits,
                    jsonExport = jsonExport,
                    totalSymbols = symbols.size,
                    totalClasses = classes.size,
                    totalMethods = classes.sumOf { it.methods.size + it.constructors.size + it.destructors.size + it.staticMethods.size },
                    totalNamespaces = namespaces.size,
                    dumpDurationMs = System.currentTimeMillis() - startTime
                )

                _uiState.value = _uiState.value.copy(
                    isDumping = false,
                    dumpProgress = null,
                    dumpResult = result
                )

                notificationHelper.cancelDumpProgress()
                notificationHelper.showDumpComplete(dumpPath)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDumping = false,
                    dumpProgress = null,
                    error = "Dump failed: ${e.message}"
                )
                notificationHelper.cancelDumpProgress()
                notificationHelper.showDumpError(e.message ?: "Unknown error")
            }
        }
    }

    fun cancelDump() {
        _uiState.value = _uiState.value.copy(
            isDumping = false,
            dumpProgress = null
        )
        notificationHelper.cancelDumpProgress()
    }

    fun clearDumpResult() {
        _uiState.value = _uiState.value.copy(dumpResult = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun generateDumpInfo(
        elfInfo: ElfInfo,
        symbols: List<com.neomods.libdumper.domain.Symbol>,
        classes: List<com.neomods.libdumper.domain.ClassInfo>,
        namespaces: List<com.neomods.libdumper.domain.NamespaceInfo>,
        startTime: Long
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val duration = System.currentTimeMillis() - startTime

        return """
            |Lib Dumper - Dump Information
            |============================
            |
            |Library Name: ${elfInfo.fileName}
            |Architecture: ${elfInfo.architecture}
            |File Size: ${FileUtils.formatFileSize(elfInfo.fileSize)}
            |Build Time: ${dateFormat.format(Date())}
            |
            |Total Symbols: ${symbols.size}
            |Recovered Classes: ${classes.size}
            |Recovered Methods: ${classes.sumOf { it.methods.size + it.constructors.size + it.destructors.size + it.staticMethods.size }}
            |Recovered Namespaces: ${namespaces.size}
            |
            |Dump Duration: ${duration}ms
        """.trimMargin()
    }

    private fun generateCredits(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return """
            |Lib Dumper
            |Generated by Lib Dumper
            |
            |Instructions:
            |Use this application responsibly.
            |The developer is not responsible for misuse of generated output.
            |
            |Developer Contact:
            | https://t.me/NeoModsChannel
            |
            |Community:
            | https://t.me/+RYSsITD6K-U4NzI0 /* url might be outdated, join channel to find latest bia url above */
            |
            |Timestamp:
            |${dateFormat.format(Date())}
            |
            |Version:
            |1.0.0
        """.trimMargin()
    }

    private suspend fun saveDumpFiles(
        context: Context,
        elfInfo: ElfInfo,
        dumpCpp: String?,
        symbolTable: String?,
        dumpInfo: String?,
        credits: String?,
        jsonExport: String?,
        totalSymbols: Int,
        totalClasses: Int,
        totalMethods: Int,
        totalNamespaces: Int,
        durationMs: Long
    ): String {
        val basePath = settingsManager.dumpLocation.first()
        val dumpIndex = FileUtils.findNextAvailableDumpIndex(basePath)
        val dumpDir = FileUtils.createDumpDirectory(basePath, dumpIndex)

        dumpCpp?.let {
            FileUtils.writeToFile(File(dumpDir, "Dump.cpp"), it)
        }

        symbolTable?.let {
            FileUtils.writeToFile(File(dumpDir, "SymbolTable.txt"), it)
        }

        dumpInfo?.let {
            FileUtils.writeToFile(File(dumpDir, "DumpInfo.txt"), it)
        }

        credits?.let {
            FileUtils.writeToFile(File(dumpDir, "Credits.txt"), it)
        }

        jsonExport?.let {
            FileUtils.writeToFile(File(dumpDir, "dump.json"), it)
        }

        return dumpDir.absolutePath
    }
}
