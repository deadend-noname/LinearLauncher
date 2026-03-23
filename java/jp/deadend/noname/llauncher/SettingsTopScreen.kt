package jp.deadend.noname.llauncher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToHiddenApps: () -> Unit
) {
    val recentLimit by viewModel.recentAppsLimit.collectAsState()
    val exitOnLaunch by viewModel.exitOnLaunch.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                ),
                title = { Text(stringResource(R.string.appbar_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.appbar_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_hidden_apps)) },
                modifier = Modifier.clickable { onNavigateToHiddenApps() }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_recent_count)) },
                supportingContent = { Text(stringResource(R.string.settings_sup_recent_count, recentLimit)) },
                modifier = Modifier.clickable { showDialog = true }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_exit_on_launch)) },
                supportingContent = { Text(stringResource(R.string.settinng_sup_exit_on_launch)) },
                trailingContent = {
                    Switch(
                        checked = exitOnLaunch,
                        onCheckedChange = { viewModel.setExitOnLaunch(it) }
                    )
                },
                modifier = Modifier.clickable { viewModel.setExitOnLaunch(!exitOnLaunch) }
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_recent_count)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items((0..20).toList()) { num ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setRecentAppsLimit(num)
                                    showDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (num == recentLimit), onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = num.toString())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}