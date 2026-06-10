package com.neomods.libdumper.ui.screens.main

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neomods.libdumper.domain.DumpConfig
import com.neomods.libdumper.domain.DumpProgress
import com.neomods.libdumper.domain.ElfInfo
import com.neomods.libdumper.utils.FileUtils

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainDumperScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val dumpConfig by viewModel.dumpConfig.collectAsState()
    val recentLibraries by viewModel.recentLibraries.collectAsState()

    var showProgressDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.selectLibrary(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lib Dumper",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                SelectedLibraryCard(
                    elfInfo = uiState.elfInfo,
                    onSelectLibrary = {
                        filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    }
                )
            }

            if (recentLibraries.isNotEmpty()) {
                item {
                    RecentLibrariesCard(
                        libraries = recentLibraries,
                        onLibrarySelected = { path ->
                            viewModel.selectLibraryByPath(context, path)
                        }
                    )
                }
            }

            item {
                ExpandableConfigCard(
                    title = "Symbol Sources",
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple("Extract .symtab", dumpConfig.extractSymtab) { v -> dumpConfig.copy(extractSymtab = v) },
                        Triple("Extract .dynsym", dumpConfig.extractDynsym) { v -> dumpConfig.copy(extractDynsym = v) },
                        Triple("Exported symbols", dumpConfig.extractExported) { v -> dumpConfig.copy(extractExported = v) },
                        Triple("Imported symbols", dumpConfig.extractImported) { v -> dumpConfig.copy(extractImported = v) },
                        Triple("Raw symbol names", dumpConfig.dumpRawNames) { v -> dumpConfig.copy(dumpRawNames = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ExpandableConfigCard(
                    title = "Reconstruction",
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple("C++ reconstruction", dumpConfig.generateCppReconstruction) { v -> dumpConfig.copy(generateCppReconstruction = v) },
                        Triple("Group methods into classes", dumpConfig.groupMethodsIntoClasses) { v -> dumpConfig.copy(groupMethodsIntoClasses = v) },
                        Triple("Group static methods", dumpConfig.groupStaticMethods) { v -> dumpConfig.copy(groupStaticMethods = v) },
                        Triple("Detect constructors", dumpConfig.detectConstructors) { v -> dumpConfig.copy(detectConstructors = v) },
                        Triple("Detect destructors", dumpConfig.detectDestructors) { v -> dumpConfig.copy(detectDestructors = v) },
                        Triple("Detect overloaded methods", dumpConfig.detectOverloadedMethods) { v -> dumpConfig.copy(detectOverloadedMethods = v) },
                        Triple("Detect namespaces", dumpConfig.detectNamespaces) { v -> dumpConfig.copy(detectNamespaces = v) },
                        Triple("Generate comments", dumpConfig.generateComments) { v -> dumpConfig.copy(generateComments = v) },
                        Triple("Method signatures", dumpConfig.includeMethodSignatures) { v -> dumpConfig.copy(includeMethodSignatures = v) },
                        Triple("Return types", dumpConfig.includeReturnTypes) { v -> dumpConfig.copy(includeReturnTypes = v) },
                        Triple("Parameter types", dumpConfig.includeParameterTypes) { v -> dumpConfig.copy(includeParameterTypes = v) },
                        Triple("Inheritance detection", dumpConfig.attemptInheritanceDetection) { v -> dumpConfig.copy(attemptInheritanceDetection = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ExpandableConfigCard(
                    title = "Address Info",
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple("Virtual addresses", dumpConfig.includeVirtualAddresses) { v -> dumpConfig.copy(includeVirtualAddresses = v) },
                        Triple("RVA", dumpConfig.includeRva) { v -> dumpConfig.copy(includeRva = v) },
                        Triple("File offsets", dumpConfig.includeFileOffsets) { v -> dumpConfig.copy(includeFileOffsets = v) },
                        Triple("Symbol sizes", dumpConfig.includeSymbolSizes) { v -> dumpConfig.copy(includeSymbolSizes = v) },
                        Triple("Section names", dumpConfig.includeSectionNames) { v -> dumpConfig.copy(includeSectionNames = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ExpandableConfigCard(
                    title = "Output Files",
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple("Dump.cpp", dumpConfig.generateDumpCpp) { v -> dumpConfig.copy(generateDumpCpp = v) },
                        Triple("SymbolTable.txt", dumpConfig.generateSymbolTable) { v -> dumpConfig.copy(generateSymbolTable = v) },
                        Triple("Credits.txt", dumpConfig.generateCredits) { v -> dumpConfig.copy(generateCredits = v) },
                        Triple("DumpInfo.txt", dumpConfig.generateDumpInfo) { v -> dumpConfig.copy(generateDumpInfo = v) },
                        Triple("JSON export", dumpConfig.generateJson) { v -> dumpConfig.copy(generateJson = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                Button(
                    onClick = {
                        showProgressDialog = true
                        viewModel.startDump(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = uiState.elfInfo != null && !uiState.isDumping
                ) {
                    Text(
                        text = "Generate Dump",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }

    if (showProgressDialog && uiState.isDumping) {
        DumpProgressDialog(
            progress = uiState.dumpProgress,
            onDismiss = {
                showProgressDialog = false
                viewModel.cancelDump()
            }
        )
    }

    uiState.dumpResult?.let { result ->
        if (showProgressDialog) {
            DumpCompleteDialog(
                result = result,
                onDismiss = {
                    showProgressDialog = false
                    viewModel.clearDumpResult()
                }
            )
        }
    }

    uiState.error?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
fun SelectedLibraryCard(
    elfInfo: ElfInfo?,
    onSelectLibrary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Library",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (elfInfo != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Loaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (elfInfo != null) {
                CompactInfoRow("Name", elfInfo.fileName)
                CompactInfoRow("Arch", elfInfo.architecture)
                CompactInfoRow("Size", FileUtils.formatFileSize(elfInfo.fileSize))
                CompactInfoRow(
                    "Status",
                    if (elfInfo.isValid) "Valid ELF" else "Invalid",
                    valueColor = if (elfInfo.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "No library selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSelectLibrary,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Select Library", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CompactInfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
        )
    }
}

@Composable
fun RecentLibrariesCard(
    libraries: List<String>,
    onLibrarySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Recent Libraries",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            libraries.filter { !it.isNullOrBlank() }.take(5).forEach { path ->
                val fileName = path.substringAfterLast("/")
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLibrarySelected(path) }
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ExpandableConfigCard(
    title: String,
    config: DumpConfig,
    checkboxes: List<Triple<String, Boolean, (Boolean) -> DumpConfig>>,
    onConfigChange: (DumpConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val enabledCount = checkboxes.count { it.second }
                Text(
                    text = "$enabledCount/${checkboxes.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    checkboxes.forEach { (label, checked, updater) ->
                        CompactCheckboxRow(
                            label = label,
                            checked = checked,
                            onCheckedChange = { onConfigChange(updater(it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DumpProgressDialog(
    progress: DumpProgress?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = "Dumping Library", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    text = progress?.stage ?: "Starting...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = progress?.message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress?.progress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${((progress?.progress ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun DumpCompleteDialog(
    result: com.neomods.libdumper.domain.DumpResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Dump Complete", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Library: ${result.elfInfo.fileName}", style = MaterialTheme.typography.bodySmall)
                Text("Symbols: ${result.totalSymbols}", style = MaterialTheme.typography.bodySmall)
                Text("Classes: ${result.totalClasses}", style = MaterialTheme.typography.bodySmall)
                Text("Methods: ${result.totalMethods}", style = MaterialTheme.typography.bodySmall)
                Text("Namespaces: ${result.totalNamespaces}", style = MaterialTheme.typography.bodySmall)
                Text("Duration: ${result.dumpDurationMs}ms", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        }
    )
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Error", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodySmall)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        }
    )
}
