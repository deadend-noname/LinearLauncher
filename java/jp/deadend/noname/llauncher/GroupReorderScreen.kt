package jp.deadend.noname.llauncher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReorderScreen(
    groups: List<GroupEntity>,
    onMoveUp: (GroupEntity) -> Unit,
    onMoveDown: (GroupEntity) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                ),
                title = { Text(stringResource(R.string.label_reorder_tabs)) },
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
            itemsIndexed(groups, key = { _, item -> item.groupId }) { index, group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(
                        onClick = { onMoveUp(group) },
                        enabled = index > 0
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_double_arrow_up_24px),
                            contentDescription = stringResource(R.string.label_up),
                        )
                    }

                    IconButton(
                        onClick = { onMoveDown(group) },
                        enabled = index < groups.size - 1
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_double_arrow_down_24px),
                            contentDescription = stringResource(R.string.label_down),
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}