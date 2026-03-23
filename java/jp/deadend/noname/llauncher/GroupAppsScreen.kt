package jp.deadend.noname.llauncher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAppsScreen(
    groupName: String,
    allApps: List<AppEntity>,
    appsInGroup: List<AppEntity>,
    onToggleApp: (AppEntity, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val inGroupPackageNames = appsInGroup.map { it.packageName }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                ),
                title = { Text(stringResource(R.string.title_apps_in_group, groupName)) },
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(allApps, key = { it.packageName }) { app ->
                val isIncluded = inGroupPackageNames.contains(app.packageName)
                val isHidden = app.isHidden
                val rowAlpha = if (isHidden) 0.5f else 1.0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isHidden) { onToggleApp(app, !isIncluded) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .alpha(rowAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = AppIconData(app.packageName),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )
                        if (isHidden) {
                            Text(
                                text = stringResource(R.string.label_hidden),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Checkbox(
                        checked = isIncluded,
                        onCheckedChange = { checked -> onToggleApp(app, checked) },
                        enabled = !isHidden
                    )
                }
                HorizontalDivider()
            }
        }
    }
}