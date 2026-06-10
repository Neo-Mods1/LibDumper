package com.neomods.libdumper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.neomods.libdumper.domain.ThemeMode
import com.neomods.libdumper.storage.SettingsManager
import com.neomods.libdumper.ui.navigation.LibDumperNavGraph
import com.neomods.libdumper.ui.theme.LibDumperTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lang = settingsManager.language.value
        applyLocale(lang)

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()
            val currentLang by settingsManager.language.collectAsState()

            LaunchedEffect(currentLang) {
                applyLocale(currentLang)
            }

            LibDumperTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LibDumperNavGraph()
                }
            }
        }
    }

    private fun applyLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
