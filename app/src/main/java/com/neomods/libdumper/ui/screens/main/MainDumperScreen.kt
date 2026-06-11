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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neomods.libdumper.R
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
                        text = stringResource(R.string.main_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.about),
                            modifier = Modifier.size(24.dp)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    title = stringResource(R.string.symbol_sources),
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple(stringResource(R.string.extract_symtab), dumpConfig.extractSymtab) { v -> dumpConfig.copy(extractSymtab = v) },
                        Triple(stringResource(R.string.extract_dynsym), dumpConfig.extractDynsym) { v -> dumpConfig.copy(extractDynsym = v) },
                        Triple(stringResource(R.string.exported_symbols), dumpConfig.extractExported) { v -> dumpConfig.copy(extractExported = v) },
                        Triple(stringResource(R.string.imported_symbols), dumpConfig.extractImported) { v -> dumpConfig.copy(extractImported = v) },
                        Triple(stringResource(R.string.raw_symbol_names), dumpConfig.dumpRawNames) { v -> dumpConfig.copy(dumpRawNames = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ExpandableConfigCard(
                    title = stringResource(R.string.reconstruction),
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple(stringResource(R.string.cpp_reconstruction), dumpConfig.generateCppReconstruction) { v -> dumpConfig.copy(generateCppReconstruction = v) },
                        Triple(stringResource(R.string.group_methods_into_classes), dumpConfig.groupMethodsIntoClasses) { v -> dumpConfig.copy(groupMethodsIntoClasses = v) },
                        Triple(stringResource(R.string.group_static_methods), dumpConfig.groupStaticMethods) { v -> dumpConfig.copy(groupStaticMethods = v) },
                        Triple(stringResource(R.string.detect_constructors), dumpConfig.detectConstructors) { v -> dumpConfig.copy(detectConstructors = v) },
                        Triple(stringResource(R.string.detect_destructors), dumpConfig.detectDestructors) { v -> dumpConfig.copy(detectDestructors = v) },
                        Triple(stringResource(R.string.detect_overloaded_methods), dumpConfig.detectOverloadedMethods) { v -> dumpConfig.copy(detectOverloadedMethods = v) },
                        Triple(stringResource(R.string.detect_namespaces), dumpConfig.detectNamespaces) { v -> dumpConfig.copy(detectNamespaces = v) },
                        Triple(stringResource(R.string.generate_comments), dumpConfig.generateComments) { v -> dumpConfig.copy(generateComments = v) },
                        Triple(stringResource(R.string.method_signatures), dumpConfig.includeMethodSignatures) { v -> dumpConfig.copy(includeMethodSignatures = v) },
                        Triple(stringResource(R.string.return_types), dumpConfig.includeReturnTypes) { v -> dumpConfig.copy(includeReturnTypes = v) },
                        Triple(stringResource(R.string.parameter_types), dumpConfig.includeParameterTypes) { v -> dumpConfig.copy(includeParameterTypes = v) },
                        Triple(stringResource(R.string.inheritance_detection), dumpConfig.attemptInheritanceDetection) { v -> dumpConfig.copy(attemptInheritanceDetection = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ExpandableConfigCard(
                    title = stringResource(R.string.address_info),
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple(stringResource(R.string.virtual_addresses), dumpConfig.includeVirtualAddresses) { v -> dumpConfig.copy(includeVirtualAddresses = v) },
                        Triple(stringResource(R.string.rva), dumpConfig.includeRva) { v -> dumpConfig.copy(includeRva = v) },
                        Triple(stringResource(R.string.file_offsets), dumpConfig.includeFileOffsets) { v -> dumpConfig.copy(includeFileOffsets = v) },
                        Triple(stringResource(R.string.symbol_sizes), dumpConfig.includeSymbolSizes) { v -> dumpConfig.copy(includeSymbolSizes = v) },
                        Triple(stringResource(R.string.section_names), dumpConfig.includeSectionNames) { v -> dumpConfig.copy(includeSectionNames = v) },
                    ),
                    onConfigChange = { viewModel.updateDumpConfig(it) }
                )
            }

            item {
                ExpandableConfigCard(
                    title = stringResource(R.string.output_files),
                    config = dumpConfig,
                    checkboxes = listOf(
                        Triple(stringResource(R.string.dump_cpp_file), dumpConfig.generateDumpCpp) { v -> dumpConfig.copy(generateDumpCpp = v) },
                        Triple(stringResource(R.string.symbol_table_file), dumpConfig.generateSymbolTable) { v -> dumpConfig.copy(generateSymbolTable = v) },
                        Triple(stringResource(R.string.credits_file), dumpConfig.generateCredits) { v -> dumpConfig.copy(generateCredits = v) },
                        Triple(stringResource(R.string.dump_info_file), dumpConfig.generateDumpInfo) { v -> dumpConfig.copy(generateDumpInfo = v) },
                        Triple(stringResource(R.string.json_export), dumpConfig.generateJson) { v -> dumpConfig.copy(generateJson = v) },
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
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    enabled = uiState.elfInfo != null && !uiState.isDumping,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generate_dump),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.selected_library),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (elfInfo != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.loaded),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (elfInfo != null) {
                InfoRow(stringResource(R.string.name_label), elfInfo.fileName)
                InfoRow(stringResource(R.string.arch), elfInfo.architecture)
                InfoRow(stringResource(R.string.size), FileUtils.formatFileSize(elfInfo.fileSize))
                InfoRow(
                    stringResource(R.string.status),
                    if (elfInfo.isValid) stringResource(R.string.valid_elf) else stringResource(R.string.invalid_elf),
                    valueColor = if (elfInfo.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = stringResource(R.string.no_library_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSelectLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.select_library), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.recent_libraries),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            libraries.filter { !it.isNullOrBlank() }.take(5).forEach { path ->
                val fileName = path?.substringAfterLast("/") ?: path ?: ""
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
fun ExpandableConfigCard(
    title: String,
    config: DumpConfig,
    checkboxes: List<Triple<String, Boolean, (Boolean) -> DumpConfig>>,
    onConfigChange: (DumpConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val enabledCount = checkboxes.count { it.second }
                Text(
                    text = "$enabledCount/${checkboxes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    checkboxes.forEach { (label, checked, updater) ->
                        CheckboxRow(
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
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(22.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
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
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = stringResource(R.string.dumping_library), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    text = progress?.stage ?: stringResource(R.string.starting),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
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
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = stringResource(R.string.dump_complete), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${stringResource(R.string.library)}: ${result.elfInfo.fileName}", style = MaterialTheme.typography.bodyMedium)
                Text("${stringResource(R.string.symbols)}: ${result.totalSymbols}", style = MaterialTheme.typography.bodyMedium)
                Text("${stringResource(R.string.classes)}: ${result.totalClasses}", style = MaterialTheme.typography.bodyMedium)
                Text("${stringResource(R.string.methods)}: ${result.totalMethods}", style = MaterialTheme.typography.bodyMedium)
                Text("${stringResource(R.string.namespaces)}: ${result.totalNamespaces}", style = MaterialTheme.typography.bodyMedium)
                Text("${stringResource(R.string.duration)}: ${result.dumpDurationMs}ms", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok), style = MaterialTheme.typography.labelLarge)
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
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = stringResource(R.string.error), style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok), style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
