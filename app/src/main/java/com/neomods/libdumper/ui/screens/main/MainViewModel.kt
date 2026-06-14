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
                if (!NativeLibWrapper.isNativeAvailable()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Native library not loaded. The app may not have the required .so files for your device architecture."
                    )
                    return@launch
                }

                val fileName = FileUtils.getFileName(context, uri)
                
                if (!fileName.endsWith(".so")) {
                    _uiState.value = _uiState.value.copy(
                        error = "Selected file is not a .so library"
                    )
                    return@launch
                }

                val elfInfo = withContext(Dispatchers.IO) {
                    val tempFile = FileUtils.copyFileToInternal(context, uri, fileName)
                    if (!tempFile.exists() || tempFile.length() == 0L) {
                        throw IllegalStateException("Failed to copy file to internal storage")
                    }
                    currentFilePath = tempFile.absolutePath
                    NativeLibWrapper.loadElf(tempFile.absolutePath)
                }

                if (elfInfo != null) {
                    _uiState.value = _uiState.value.copy(elfInfo = elfInfo)
                    currentUri = uri
                    settingsManager.addRecentLibrary(elfInfo.filePath)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load ELF file. The file may be corrupt or not a valid ELF."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error selecting library: ${e.message ?: e.toString()}"
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
            _uiState.value = _uiState.value.copy(error = "No library selected")
            return
        }

        val elfInfo = _uiState.value.elfInfo ?: run {
            _uiState.value = _uiState.value.copy(error = "No library selected")
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
                val config = _dumpConfig.value

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Loading ELF", 0.1f, "Loading ELF file...")
                )
                notificationHelper.showDumpProgress("Loading ELF", 10)

                val symbolCount = withContext(Dispatchers.IO) {
                    NativeLibWrapper.extractSymbols(filePath, config)
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Reading Symbols", 0.3f, "Extracted $symbolCount symbols")
                )
                notificationHelper.showDumpProgress("Reading Symbols", 30)

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Detecting Classes", 0.5f, "Analyzing class structures...")
                )
                notificationHelper.showDumpProgress("Detecting Classes", 50)

                val classCount = withContext(Dispatchers.IO) {
                    NativeLibWrapper.reconstructClasses(config)
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Detecting Namespaces", 0.6f, "Found $classCount classes")
                )
                notificationHelper.showDumpProgress("Detecting Namespaces", 60)

                val namespaceCount = withContext(Dispatchers.IO) {
                    NativeLibWrapper.detectNamespaces(config)
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Building Structures", 0.7f, "Found $namespaceCount namespaces")
                )
                notificationHelper.showDumpProgress("Building Structures", 70)

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Generating Files", 0.8f, "Generating output files...")
                )
                notificationHelper.showDumpProgress("Generating Files", 80)

                val dumpCpp = if (config.generateDumpCpp) {
                    withContext(Dispatchers.IO) {
                        NativeLibWrapper.generateDumpCpp(config)
                    }
                } else null

                val symbolTable = if (config.generateSymbolTable) {
                    withContext(Dispatchers.IO) {
                        NativeLibWrapper.generateSymbolTable(config)
                    }
                } else null

                val jsonExport = if (config.generateJson) {
                    withContext(Dispatchers.IO) {
                        NativeLibWrapper.generateJsonExport(elfInfo)
                    }
                } else null

                val dumpInfo = if (config.generateDumpInfo) {
                    generateDumpInfo(elfInfo, symbolCount, classCount, namespaceCount, startTime)
                } else null

                val credits = if (config.generateCredits) {
                    generateCredits()
                } else null

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
                        jsonExport
                    )
                }

                val result = DumpResult(
                    elfInfo = elfInfo,
                    dumpCpp = dumpCpp,
                    symbolTable = symbolTable,
                    dumpInfo = dumpInfo,
                    credits = credits,
                    jsonExport = jsonExport,
                    totalSymbols = symbolCount,
                    totalClasses = classCount,
                    totalMethods = 0,
                    totalNamespaces = namespaceCount,
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
        symbolCount: Int,
        classCount: Int,
        namespaceCount: Int,
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
            |Total Symbols: $symbolCount
            |Recovered Classes: $classCount
            |Recovered Namespaces: $namespaceCount
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
            | https://t.me/+RYSsITD6K-U4NzI0
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
        jsonExport: String?
    ): String {
        val basePath = settingsManager.dumpLocation.first()
        val dumpIndex = FileUtils.findNextAvailableDumpIndex(basePath)
        val dumpDir = FileUtils.createDumpDirectory(basePath, dumpIndex)

        dumpCpp?.let { FileUtils.writeToFile(File(dumpDir, "Dump.cpp"), it) }
        symbolTable?.let { FileUtils.writeToFile(File(dumpDir, "SymbolTable.txt"), it) }
        dumpInfo?.let { FileUtils.writeToFile(File(dumpDir, "DumpInfo.txt"), it) }
        credits?.let { FileUtils.writeToFile(File(dumpDir, "Credits.txt"), it) }
        jsonExport?.let { FileUtils.writeToFile(File(dumpDir, "dump.json"), it) }

        return dumpDir.absolutePath
    }
}
