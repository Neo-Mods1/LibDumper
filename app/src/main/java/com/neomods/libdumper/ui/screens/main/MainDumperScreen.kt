package com.neomods.libdumper.ui.screens.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neomods.libdumper.domain.ClassInfo
import com.neomods.libdumper.domain.DumpConfig
import com.neomods.libdumper.domain.DumpProgress
import com.neomods.libdumper.domain.ElfInfo
import com.neomods.libdumper.domain.NamespaceInfo
import com.neomods.libdumper.domain.Symbol
import com.neomods.libdumper.utils.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDumperScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val dumpConfig by viewModel.dumpConfig.collectAsState()
    val recentLibraries by viewModel.recentLibraries.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                SymbolSourcesCard(
                    config = dumpConfig,
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ReconstructionOptionsCard(
                    config = dumpConfig,
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                AddressInfoCard(
                    config = dumpConfig,
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                OutputFilesCard(
                    config = dumpConfig,
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
                        .height(56.dp),
                    enabled = uiState.elfInfo != null && !uiState.isDumping
                ) {
                    Text(
                        text = "Generate Dump",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selected Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (elfInfo != null) {
                LibraryInfoRow(label = "Name", value = elfInfo.fileName)
                LibraryInfoRow(label = "Path", value = elfInfo.filePath, maxLines = 1)
                LibraryInfoRow(label = "Architecture", value = elfInfo.architecture)
                LibraryInfoRow(label = "File Size", value = FileUtils.formatFileSize(elfInfo.fileSize))
                LibraryInfoRow(
                    label = "Status",
                    value = if (elfInfo.isValid) "Valid" else "Invalid",
                    valueColor = if (elfInfo.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "No library selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSelectLibrary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Select Library")
            }
        }
    }
}

@Composable
fun LibraryInfoRow(
    label: String,
    value: String,
    maxLines: Int = 2,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Libraries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            libraries.take(5).forEach { path ->
                val fileName = path.substringAfterLast("/")
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLibrarySelected(path) }
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SymbolSourcesCard(
    config: DumpConfig,
    onConfigChange: (DumpConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Symbol Sources",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    CheckboxRow(
                        label = "Extract .symtab",
                        checked = config.extractSymtab,
                        onCheckedChange = { onConfigChange(config.copy(extractSymtab = it)) }
                    )
                    CheckboxRow(
                        label = "Extract .dynsym",
                        checked = config.extractDynsym,
                        onCheckedChange = { onConfigChange(config.copy(extractDynsym = it)) }
                    )
                    CheckboxRow(
                        label = "Extract exported symbols",
                        checked = config.extractExported,
                        onCheckedChange = { onConfigChange(config.copy(extractExported = it)) }
                    )
                    CheckboxRow(
                        label = "Extract imported symbols",
                        checked = config.extractImported,
                        onCheckedChange = { onConfigChange(config.copy(extractImported = it)) }
                    )
                    CheckboxRow(
                        label = "Dump raw symbol names",
                        checked = config.dumpRawNames,
                        onCheckedChange = { onConfigChange(config.copy(dumpRawNames = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReconstructionOptionsCard(
    config: DumpConfig,
    onConfigChange: (DumpConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reconstruction Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    CheckboxRow(
                        label = "Generate C++ reconstruction",
                        checked = config.generateCppReconstruction,
                        onCheckedChange = { onConfigChange(config.copy(generateCppReconstruction = it)) }
                    )
                    CheckboxRow(
                        label = "Group methods into classes",
                        checked = config.groupMethodsIntoClasses,
                        onCheckedChange = { onConfigChange(config.copy(groupMethodsIntoClasses = it)) }
                    )
                    CheckboxRow(
                        label = "Group static methods",
                        checked = config.groupStaticMethods,
                        onCheckedChange = { onConfigChange(config.copy(groupStaticMethods = it)) }
                    )
                    CheckboxRow(
                        label = "Detect constructors",
                        checked = config.detectConstructors,
                        onCheckedChange = { onConfigChange(config.copy(detectConstructors = it)) }
                    )
                    CheckboxRow(
                        label = "Detect destructors",
                        checked = config.detectDestructors,
                        onCheckedChange = { onConfigChange(config.copy(detectDestructors = it)) }
                    )
                    CheckboxRow(
                        label = "Detect overloaded methods",
                        checked = config.detectOverloadedMethods,
                        onCheckedChange = { onConfigChange(config.copy(detectOverloadedMethods = it)) }
                    )
                    CheckboxRow(
                        label = "Detect namespaces",
                        checked = config.detectNamespaces,
                        onCheckedChange = { onConfigChange(config.copy(detectNamespaces = it)) }
                    )
                    CheckboxRow(
                        label = "Generate comments",
                        checked = config.generateComments,
                        onCheckedChange = { onConfigChange(config.copy(generateComments = it)) }
                    )
                    CheckboxRow(
                        label = "Include method signatures",
                        checked = config.includeMethodSignatures,
                        onCheckedChange = { onConfigChange(config.copy(includeMethodSignatures = it)) }
                    )
                    CheckboxRow(
                        label = "Include return types",
                        checked = config.includeReturnTypes,
                        onCheckedChange = { onConfigChange(config.copy(includeReturnTypes = it)) }
                    )
                    CheckboxRow(
                        label = "Include parameter types",
                        checked = config.includeParameterTypes,
                        onCheckedChange = { onConfigChange(config.copy(includeParameterTypes = it)) }
                    )
                    CheckboxRow(
                        label = "Attempt inheritance detection",
                        checked = config.attemptInheritanceDetection,
                        onCheckedChange = { onConfigChange(config.copy(attemptInheritanceDetection = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun AddressInfoCard(
    config: DumpConfig,
    onConfigChange: (DumpConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Address Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    CheckboxRow(
                        label = "Include Virtual Addresses",
                        checked = config.includeVirtualAddresses,
                        onCheckedChange = { onConfigChange(config.copy(includeVirtualAddresses = it)) }
                    )
                    CheckboxRow(
                        label = "Include RVA",
                        checked = config.includeRva,
                        onCheckedChange = { onConfigChange(config.copy(includeRva = it)) }
                    )
                    CheckboxRow(
                        label = "Include File Offsets",
                        checked = config.includeFileOffsets,
                        onCheckedChange = { onConfigChange(config.copy(includeFileOffsets = it)) }
                    )
                    CheckboxRow(
                        label = "Include Symbol Sizes",
                        checked = config.includeSymbolSizes,
                        onCheckedChange = { onConfigChange(config.copy(includeSymbolSizes = it)) }
                    )
                    CheckboxRow(
                        label = "Include Section Names",
                        checked = config.includeSectionNames,
                        onCheckedChange = { onConfigChange(config.copy(includeSectionNames = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun OutputFilesCard(
    config: DumpConfig,
    onConfigChange: (DumpConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Output Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    CheckboxRow(
                        label = "Generate Dump.cpp",
                        checked = config.generateDumpCpp,
                        onCheckedChange = { onConfigChange(config.copy(generateDumpCpp = it)) }
                    )
                    CheckboxRow(
                        label = "Generate SymbolTable.txt",
                        checked = config.generateSymbolTable,
                        onCheckedChange = { onConfigChange(config.copy(generateSymbolTable = it)) }
                    )
                    CheckboxRow(
                        label = "Generate Credits.txt",
                        checked = config.generateCredits,
                        onCheckedChange = { onConfigChange(config.copy(generateCredits = it)) }
                    )
                    CheckboxRow(
                        label = "Generate DumpInfo.txt",
                        checked = config.generateDumpInfo,
                        onCheckedChange = { onConfigChange(config.copy(generateDumpInfo = it)) }
                    )
                    CheckboxRow(
                        label = "Generate JSON export",
                        checked = config.generateJson,
                        onCheckedChange = { onConfigChange(config.copy(generateJson = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
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
            Text(text = "Dumping Library")
        },
        text = {
            Column {
                Text(
                    text = progress?.stage ?: "Starting...",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progress?.message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress?.progress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${((progress?.progress ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
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
            Text(text = "Dump Complete")
        },
        text = {
            Column {
                Text(text = "Library: ${result.elfInfo.fileName}")
                Text(text = "Total Symbols: ${result.totalSymbols}")
                Text(text = "Total Classes: ${result.totalClasses}")
                Text(text = "Total Methods: ${result.totalMethods}")
                Text(text = "Total Namespaces: ${result.totalNamespaces}")
                Text(text = "Duration: ${result.dumpDurationMs}ms")
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
            Text(text = "Error")
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK")
            }
        }
    )
}
