package com.neomods.libdumper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("locale_prefs", MODE_PRIVATE)
            .getString("lang", "") ?: ""
        if (lang.isNotEmpty()) {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = newBase.resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()

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
}
