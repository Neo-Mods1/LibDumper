package com.neomods.libdumper.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neomods.libdumper.R
import com.neomods.libdumper.domain.ThemeMode

data class LanguageOption(val code: String, val displayName: String)

val supportedLanguages = listOf(
    LanguageOption("en", "English"),
    LanguageOption("es", "Español"),
    LanguageOption("pt", "Português"),
    LanguageOption("fr", "Français"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("ja", "日本語"),
    LanguageOption("zh-rCN", "中文(简体)"),
    LanguageOption("ru", "Русский"),
    LanguageOption("ar", "العربية"),
    LanguageOption("in", "Bahasa Indonesia"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentDumpLocation by viewModel.dumpLocation.collectAsState()
    val currentLang by viewModel.language.collectAsState()
    val showRestartNotice by viewModel.showRestartNotice.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showDumpLocationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showRestartNotice) {
        if (showRestartNotice) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.restart_required),
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            viewModel.dismissRestartNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.appearance),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsRow(
                            title = stringResource(R.string.theme_mode),
                            value = when (currentTheme) {
                                ThemeMode.SYSTEM -> stringResource(R.string.system_theme)
                                ThemeMode.LIGHT -> stringResource(R.string.light_theme)
                                ThemeMode.DARK -> stringResource(R.string.dark_theme)
                            },
                            onClick = { showThemeDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        SettingsRow(
                            title = stringResource(R.string.language),
                            value = supportedLanguages.find { it.code == currentLang }?.displayName ?: "English",
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.storage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsRow(
                            title = stringResource(R.string.dump_location),
                            value = currentDumpLocation,
                            onClick = { showDumpLocationDialog = true }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.contact),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ContactRow(title = stringResource(R.string.telegram)) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NeoModsChannel")))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        ContactRow(title = stringResource(R.string.discussion)) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+RYSsITD6K-U4NzI0")))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        ContactRow(title = stringResource(R.string.github)) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Neo-Mods1/Neo-Mods1")))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        ContactRow(title = stringResource(R.string.youtube)) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@neo-modsyt?si=aHEpvVllsHPxnGck")))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                viewModel.setThemeMode(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showDumpLocationDialog) {
        DumpLocationDialog(
            currentLocation = currentDumpLocation,
            onLocationSelected = { location ->
                viewModel.setDumpLocation(location)
                showDumpLocationDialog = false
            },
            onDismiss = { showDumpLocationDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLang = currentLang,
            onLanguageSelected = { lang ->
                viewModel.setLanguage(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ContactRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun LanguageDialog(
    currentLang: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val chunks = supportedLanguages.chunked(2)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(text = stringResource(R.string.language), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                chunks.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onLanguageSelected(lang.code) }
                                    .clip(RoundedCornerShape(10.dp))
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLang == lang.code,
                                    onClick = { onLanguageSelected(lang.code) },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge) }
        }
    )
}

@Composable
fun ThemeDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(text = stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                ThemeOption(text = stringResource(R.string.system_theme), selected = currentTheme == ThemeMode.SYSTEM) { onThemeSelected(ThemeMode.SYSTEM) }
                ThemeOption(text = stringResource(R.string.light_theme), selected = currentTheme == ThemeMode.LIGHT) { onThemeSelected(ThemeMode.LIGHT) }
                ThemeOption(text = stringResource(R.string.dark_theme), selected = currentTheme == ThemeMode.DARK) { onThemeSelected(ThemeMode.DARK) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge) }
        }
    )
}

@Composable
fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DumpLocationDialog(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var location by remember { mutableStateOf(currentLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(text = stringResource(R.string.dump_location), style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.dump_location), style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onLocationSelected(location) }) { Text(text = stringResource(R.string.save), style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge) }
        }
    )
}
