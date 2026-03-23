package jp.deadend.noname.llauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BottomGroupBar(
    groups: List<GroupEntity>,
    currentGroupId: Int?,
    onGroupSelect: (Int?) -> Unit,
    onGroupLongClick: (GroupEntity) -> Unit,
    onAddGroupClick: () -> Unit
) {
    val selectedIndex = if (currentGroupId == null) {
        0
    } else {
        groups.indexOfFirst { it.groupId == currentGroupId } + 1
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.weight(1f),
            edgePadding = 8.dp,
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White,
            divider = {}
        ) {
            Tab(
                selected = currentGroupId == null,
                onClick = { onGroupSelect(null) },
                text = { Text(stringResource(R.string.label_tab_default)) }
            )

            groups.forEach { group ->
                Box(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onGroupSelect(group.groupId) },
                            onLongClick = { onGroupLongClick(group) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                ) {
                    val textColor = if (currentGroupId == group.groupId) Color.White else Color.Gray
                    Text(text = group.name, color = textColor)
                }
            }
        }

        IconButton(
            onClick = onAddGroupClick,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                painter = painterResource(R.drawable.add_24px),
                contentDescription = stringResource(R.string.label_add_group),
                tint = Color.White
            )
        }
    }
}