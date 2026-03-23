package jp.deadend.noname.llauncher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun GroupEditDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onEditApps: (() -> Unit)? = null,
    onReorderGroup: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialName.isEmpty()) {
                    stringResource(R.string.label_add_group)
                } else {
                    stringResource(R.string.label_edit_group)
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.label_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onEditApps != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onEditApps,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.label_set_apps_in_group))
                    }
                }

                if (onReorderGroup != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReorderGroup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.label_reorder_tabs))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.label_delete_group))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}