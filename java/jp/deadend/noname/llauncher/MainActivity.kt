package jp.deadend.noname.llauncher

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import jp.deadend.noname.llauncher.ui.theme.LinearLauncherTheme

sealed class Screen {
    object Home : Screen()
    object SettingsTop : Screen()
    object HiddenAppsSettings : Screen()
    object GroupOrderSettings : Screen()
    data class GroupAppsSettings(val groupId: Int, val groupName: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false)
        }

        val viewModel: MainViewModel by viewModels()

        setContent {
            var currentScreen: Screen by remember { mutableStateOf(Screen.Home) }

            LinearLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                ) {
                    when (val screen = currentScreen) {
                        Screen.Home -> AppLauncherRoute(
                            activity = this,
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = Screen.SettingsTop },
                            onNavigateToGroupApps = { group ->
                                currentScreen = Screen.GroupAppsSettings(group.groupId, group.name)
                            },
                            onNavigateToReorderGroups = { currentScreen = Screen.GroupOrderSettings }
                        )
                        Screen.SettingsTop -> SettingsTopScreen(
                            viewModel = viewModel,
                            onBackClick = { currentScreen = Screen.Home },
                            onNavigateToHiddenApps = { currentScreen = Screen.HiddenAppsSettings }
                        )
                        Screen.HiddenAppsSettings -> HiddenAppsSettingScreen(
                            allApps = viewModel.allApps.collectAsState().value,
                            onBackClick = { currentScreen = Screen.SettingsTop },
                            onSetVisibility = { app, isVisible ->
                                viewModel.setAppVisibility(app, isVisible)
                            }
                        )
                        is Screen.GroupOrderSettings -> {
                            val groups by viewModel.groups.collectAsState()
                            GroupReorderScreen(
                                groups = groups,
                                onMoveUp = viewModel::moveGroupUp,
                                onMoveDown = viewModel::moveGroupDown,
                                onBackClick = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.GroupAppsSettings-> {
                            val allApps by viewModel.allApps.collectAsState()
                            val appsInGroup by viewModel.getAppsForGroup(screen.groupId).collectAsState(initial = emptyList())

                            GroupAppsScreen(
                                groupName = screen.groupName,
                                allApps = allApps,
                                appsInGroup = appsInGroup,
                                onToggleApp = { app, isIncluded ->
                                    viewModel.toggleAppInGroup(screen.groupId, app, isIncluded)
                                },
                                onBackClick = { currentScreen = Screen.Home }
                            )
                        }
                    }

                    if (currentScreen != Screen.Home) {
                        BackHandler {
                            currentScreen = when (currentScreen) {
                                Screen.HiddenAppsSettings -> Screen.SettingsTop
                                else -> Screen.Home
                            }
                        }
                    }
                }
            }
        }
    }
}
