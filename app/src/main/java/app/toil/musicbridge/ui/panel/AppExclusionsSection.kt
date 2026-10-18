package app.toil.musicbridge.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.toil.musicbridge.MusicBridgeApplication
import app.toil.musicbridge.R
import app.toil.musicbridge.data.SelectableApp
import app.toil.musicbridge.data.SettingsRepository
import app.toil.musicbridge.data.loadSelectableApps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun LazyListScope.appExclusionsSection() {
    item(key = "app-exclusions") { AppExclusionsCard() }
}

@Composable
private fun AppExclusionsCard() {
    val settings = (LocalContext.current.applicationContext as MusicBridgeApplication).settings
    val excludedPackages by settings.excludedPackages.collectAsStateWithLifecycle(initialValue = null)
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        onClick = { showDialog = true },
        enabled = excludedPackages != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.app_exclusions_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.app_exclusions_description), style = MaterialTheme.typography.bodyMedium)
            excludedPackages?.let {
                Text(stringResource(R.string.app_exclusions_count, it.size), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    excludedPackages?.let { packages ->
        if (showDialog) AppExclusionsDialog(settings, packages, onDismiss = { showDialog = false })
    }
}

@Composable
private fun AppExclusionsDialog(settings: SettingsRepository, excludedPackages: Set<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable(stateSaver = listSaver<Set<String>, String>(
        save = { it.toList() },
        restore = { it.toSet() },
    )) { mutableStateOf(excludedPackages) }
    var query by rememberSaveable { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<SelectableApp>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(loadAttempt) {
        loadFailed = false
        try {
            apps = withContext(Dispatchers.IO) { loadSelectableApps(context, excludedPackages) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }
    val filtered = remember(apps, query) {
        val search = query.trim()
        apps?.filter { it.label.contains(search, ignoreCase = true) || it.packageName.contains(search, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.app_exclusions_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(stringResource(R.string.app_exclusions_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text(stringResource(R.string.app_exclusions_search)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item { Text(stringResource(R.string.app_exclusions_count, selected.size)) }
                    when {
                        loadFailed -> item {
                            Column {
                                Text(stringResource(R.string.app_exclusions_load_error))
                                TextButton(onClick = { loadAttempt++ }) { Text(stringResource(R.string.app_exclusions_retry)) }
                            }
                        }
                        filtered == null -> item { CircularProgressIndicator() }
                        filtered.isEmpty() -> item {
                            Column {
                                Text(stringResource(R.string.app_exclusions_no_results))
                                if (query.isNotEmpty()) {
                                    TextButton(onClick = { query = "" }) { Text(stringResource(R.string.app_exclusions_clear_search)) }
                                }
                            }
                        }
                        else -> items(filtered, key = { it.packageName }) { app ->
                            val checked = app.packageName in selected
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .toggleable(value = checked, enabled = !saving, role = Role.Checkbox) { excluded ->
                                        selected = if (excluded) selected + app.packageName else selected - app.packageName
                                        saveFailed = false
                                    }
                                    .heightIn(min = 64.dp)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null, enabled = !saving)
                                Column(Modifier.weight(1f)) {
                                    Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (saveFailed) {
                    Text(
                        stringResource(R.string.app_exclusions_save_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = apps != null && !saving,
                onClick = {
                    saving = true
                    saveFailed = false
                    scope.launch {
                        try {
                            settings.setExcludedPackages(selected)
                            onDismiss()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            saveFailed = true
                        } finally {
                            saving = false
                        }
                    }
                },
            ) { Text(stringResource(if (saving) R.string.app_exclusions_saving else R.string.app_exclusions_save)) }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.scrobbling_cancel)) }
        },
    )
}
