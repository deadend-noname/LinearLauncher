package jp.deadend.noname.llauncher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun AppListItem(
    app: AppEntity,
    rowHeight: Int,
    textColor: Color = Color.White,
    textShadow: Shadow? = null,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit = { }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .height(rowHeight.dp)
            .combinedClickable(
                onClick = { onAppClick(app.packageName) },
                onLongClick = { onAppLongClick(app.packageName) }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = AppIconData(app.packageName),
            contentDescription = "App Icon",
            modifier = Modifier.size((rowHeight * 0.65f).dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = app.label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (rowHeight * 0.5f).sp,
                shadow = textShadow
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen(
    appList: List<AppEntity>,
    rowHeight: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onSizeFinalize: () -> Unit,
    onSettingsClick: () -> Unit,
    groups: List<GroupEntity>,
    currentGroupId: Int?,
    onGroupSelect: (Int?) -> Unit,
    onAddGroup: (String) -> Unit,
    onUpdateGroup: (GroupEntity) -> Unit,
    onDeleteGroup: (GroupEntity) -> Unit,
    onNavigateToGroupApps: (GroupEntity) -> Unit,
    onNavigateToReorderGroups: () -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var targetGroup by remember { mutableStateOf<GroupEntity?>(null) }

    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    listState.scrollToItem(0)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentGroupId) {
        listState.scrollToItem(0)
    }

    val textShadow = Shadow(
        color = Color.Black,
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
    val contentColor = Color.White

    if (isSearchActive) {
        BackHandler {
            isSearchActive = false
            onSearchQueryChange("")
            focusManager.clearFocus()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.4f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        actionIconContentColor = contentColor,
                        navigationIconContentColor = contentColor
                    ),
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text(stringResource(R.string.appbar_search_placeholder)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { event ->
                                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                            if (appList.size == 1) {
                                                onAppClick(appList.first().packageName)
                                                return@onPreviewKeyEvent true
                                            }
                                        }
                                        false
                                    },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = contentColor,
                                    unfocusedTextColor = contentColor,
                                    cursorColor = contentColor,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,

                                    focusedPlaceholderColor = contentColor.copy(alpha = 0.7f),
                                    unfocusedPlaceholderColor = contentColor.copy(alpha = 0.7f),
                                )
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.app_name),
                                color = contentColor,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    shadow = textShadow
                                ),
                            )
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                onSearchQueryChange("")
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = stringResource(R.string.appbar_search),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.search_24px),
                                    contentDescription = stringResource(R.string.appbar_search),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    painter = painterResource(R.drawable.settings_24px),
                                    contentDescription = stringResource(R.string.appbar_settings),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                BottomGroupBar(
                    groups = groups,
                    currentGroupId = currentGroupId,
                    onGroupSelect = onGroupSelect,
                    onGroupLongClick = { group ->
                        targetGroup = group
                        showEditDialog = true
                    },
                    onAddGroupClick = {
                        targetGroup = null
                        showEditDialog = true
                    }
                )
            }
        ) { paddingValues ->
            if (appList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.search_not_found, searchQuery),
                            color = contentColor,
                            style = TextStyle(shadow = textShadow)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.app_not_found),
                            color = contentColor,
                            style = TextStyle(shadow = textShadow)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val zoom = event.calculateZoom()

                                    if (zoom != 1f) {
                                        onSizeChange(zoom)

                                        event.changes.forEach {
                                            if (it.positionChanged()) {
                                                it.consume()
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                onSizeFinalize()
                            }
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    val viewportHeight =
                                        listState.layoutInfo.viewportSize.height.toFloat()

                                    when (event.key) {
                                        Key.PageDown -> {
                                            scope.launch {
                                                listState.animateScrollBy(viewportHeight)
                                            }
                                            true
                                        }
                                        Key.PageUp -> {
                                            scope.launch {
                                                listState.animateScrollBy(-viewportHeight)
                                            }
                                            true
                                        }
                                        Key.MoveHome -> {
                                            scope.launch { listState.scrollToItem(0) }
                                            true
                                        }
                                        Key.MoveEnd -> {
                                            scope.launch { listState.scrollToItem(appList.size - 1) }
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                    ) {
                        items(appList, key = {it.packageName} ) { app ->
                            AppListItem(
                                app = app,
                                rowHeight = rowHeight,
                                textColor = contentColor,
                                textShadow = textShadow,
                                onAppClick = onAppClick,
                                onAppLongClick = onAppLongClick
                            )
                            HorizontalDivider(color = contentColor.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            GroupEditDialog(
                initialName = targetGroup?.name ?: "",
                onDismiss = { showEditDialog = false },
                onConfirm = { newName ->
                    if (targetGroup == null) {
                        onAddGroup(newName)
                    } else {
                        onUpdateGroup(targetGroup!!.copy(name = newName))
                    }
                    showEditDialog = false
                },
                onDelete = if (targetGroup != null) {
                    {
                        onDeleteGroup(targetGroup!!)
                        showEditDialog = false
                    }
                } else null,
                onEditApps = if (targetGroup != null) {
                    {
                        onNavigateToGroupApps(targetGroup!!)
                        showEditDialog = false
                    }
                } else null,
                onReorderGroup = {
                    onNavigateToReorderGroups()
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun AppLauncherRoute(
    activity: Activity,
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToGroupApps: (GroupEntity) -> Unit,
    onNavigateToReorderGroups: () -> Unit
) {
    val appList by viewModel.visibleApps.collectAsState()
    val rowHeight by viewModel.rowHeight.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val currentGroupId by viewModel.currentGroupId.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncApps()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AppLauncherScreen(
        appList = appList,
        rowHeight = rowHeight,
        searchQuery = searchQuery,
        groups = groups,
        currentGroupId = currentGroupId,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onGroupSelect = viewModel::setGroupId,
        onAddGroup = viewModel::addGroup,
        onUpdateGroup = viewModel::updateGroup,
        onDeleteGroup = viewModel::deleteGroup,
        onAppClick = { pkg ->
            viewModel.onAppLaunched(pkg) { activity.finish() }
        },
        onAppLongClick = { pkg ->
            val intent = Intent().apply {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", pkg, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        },
        onSizeChange = { delta -> viewModel.previewRowHeight(delta) },
        onSizeFinalize = { viewModel.finalizeRowHeight() },
        onSettingsClick = onNavigateToSettings,
        onNavigateToGroupApps = onNavigateToGroupApps,
        onNavigateToReorderGroups = onNavigateToReorderGroups
    )
}
