package com.neomods.libdumper.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.libdumper.domain.ThemeMode
import com.neomods.libdumper.storage.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val dumpLocation: StateFlow<String> = settingsManager.dumpLocation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "/storage/emulated/0/Dumper"
        )

    val language: StateFlow<String> = settingsManager.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )

    private val _showRestartNotice = MutableStateFlow(false)
    val showRestartNotice: StateFlow<Boolean> = _showRestartNotice.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun setDumpLocation(location: String) {
        viewModelScope.launch {
            settingsManager.setDumpLocation(location)
        }
    }

    fun setLanguage(lang: String) {
        val current = language.value
        if (lang == current) return
        viewModelScope.launch {
            settingsManager.setLanguage(lang)
            _showRestartNotice.value = true
        }
    }

    fun dismissRestartNotice() {
        _showRestartNotice.value = false
    }
}
