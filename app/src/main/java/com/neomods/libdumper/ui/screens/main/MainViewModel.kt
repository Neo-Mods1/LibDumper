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

                val basePath = settingsManager.dumpLocation.first()
                val dumpIndex = FileUtils.findNextAvailableDumpIndex(basePath)
                val dumpDir = FileUtils.createDumpDirectory(basePath, dumpIndex)
                val outputDir = dumpDir.absolutePath

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Analyzing Library", 0.3f, "Parsing ELF and extracting symbols...")
                )
                notificationHelper.showDumpProgress("Analyzing Library", 30)

                val stats = withContext(Dispatchers.IO) {
                    NativeLibWrapper.runDump(filePath, outputDir, config)
                }

                if (stats == null) {
                    _uiState.value = _uiState.value.copy(
                        isDumping = false,
                        dumpProgress = null,
                        error = "Dump failed: native pipeline returned null"
                    )
                    notificationHelper.cancelDumpProgress()
                    notificationHelper.showDumpError("Native pipeline failed")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Writing Info Files", 0.85f, "Generating DumpInfo and Credits...")
                )
                notificationHelper.showDumpProgress("Writing Info Files", 85)

                withContext(Dispatchers.IO) {
                    if (config.generateDumpInfo) {
                        val dumpInfo = generateDumpInfo(
                            elfInfo, stats.totalSymbols, stats.totalClasses,
                            stats.totalNamespaces, startTime
                        )
                        FileUtils.writeToFile(File(dumpDir, "DumpInfo.txt"), dumpInfo)
                    }
                    if (config.generateCredits) {
                        val credits = generateCredits()
                        FileUtils.writeToFile(File(dumpDir, "Credits.txt"), credits)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    dumpProgress = DumpProgress("Complete", 1.0f, "Done!")
                )

                val result = DumpResult(
                    elfInfo = elfInfo,
                    dumpCpp = null,
                    symbolTable = null,
                    dumpInfo = null,
                    credits = null,
                    jsonExport = null,
                    totalSymbols = stats.totalSymbols,
                    totalClasses = stats.totalClasses,
                    totalMethods = 0,
                    totalNamespaces = stats.totalNamespaces,
                    dumpDurationMs = System.currentTimeMillis() - startTime
                )

                _uiState.value = _uiState.value.copy(
                    isDumping = false,
                    dumpProgress = null,
                    dumpResult = result
                )

                notificationHelper.cancelDumpProgress()
                notificationHelper.showDumpComplete(outputDir)

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
            |3.0.0
        """.trimMargin()
    }
}
