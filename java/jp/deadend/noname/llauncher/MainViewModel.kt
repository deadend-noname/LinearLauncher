package jp.deadend.noname.llauncher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()

    private val settingsRepository = SettingsRepository(application)

    private val _currentGroupId = MutableStateFlow<Int?>(null)
    val currentGroupId = _currentGroupId.asStateFlow()
    val groups = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val visibleApps = _currentGroupId.flatMapLatest { groupId ->
        val appsFlow = if (groupId == null) {
            dao.getAllApps()
        } else {
            dao.getAppsByGroup(groupId)
        }

        combine(
            appsFlow,
            settingsRepository.recentAppsLimit,
            _searchQuery
        ) { apps, limit, query ->
            val baseList = apps.filter { !it.isHidden }
            val filteredList = if (query.isBlank()) {
                baseList
            } else {
                baseList.filter { it.label.contains(query, ignoreCase = true) }
            }

            val recentApps = filteredList
                .filter { it.lastLaunched > 0 }
                .sortedByDescending { it.lastLaunched }
                .take(limit)

            val recentPackageNames = recentApps.map { it.packageName }.toSet()
            val otherApps = filteredList
                .filter { it.packageName !in recentPackageNames }
                .sortedBy { it.label }

            recentApps + otherApps
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps = dao.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAppsLimit = settingsRepository.recentAppsLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val exitOnLaunch = settingsRepository.exitOnLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val savedRowHeight = settingsRepository.rowHeight
    private val _tempRowHeight = MutableStateFlow<Int?>(null)

    val rowHeight = combine(savedRowHeight, _tempRowHeight) { saved, temp ->
        temp ?: saved
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 72)

    fun setRecentAppsLimit(limit: Int) {
        viewModelScope.launch {
            settingsRepository.setRecentAppsLimit(limit)
        }
    }

    fun setExitOnLaunch(exit: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExitOnLaunch(exit)
        }
    }

    fun previewRowHeight(delta: Float) {
        val current = _tempRowHeight.value ?: rowHeight.value
        _tempRowHeight.value = (current * delta).toInt().coerceIn(30, 150)
    }

    fun finalizeRowHeight() {
        viewModelScope.launch {
            _tempRowHeight.value?.let { settingsRepository.setRowHeight(it) }
            _tempRowHeight.value = null
        }
    }

    fun setGroupId(groupId: Int?) {
        _currentGroupId.value = groupId
    }

    fun addGroup(name: String) {
        viewModelScope.launch {
            val currentList = groups.value
            val nextOrder = if (currentList.isEmpty()) 0 else currentList.maxOf { it.sortOrder } + 1
            dao.insertGroup(GroupEntity(name = name, sortOrder = nextOrder))
        }
    }

    fun updateGroup(group: GroupEntity) {
        viewModelScope.launch {
            dao.insertGroup(group)
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            dao.deleteGroup(group)
            if (_currentGroupId.value == group.groupId) {
                _currentGroupId.value = null
            }
        }
    }

    fun updateGroupsOrder(reorderedList: List<GroupEntity>) {
        viewModelScope.launch {
            val updatedList = reorderedList.mapIndexed { index, group ->
                group.copy(sortOrder = index)
            }
            dao.updateGroups(updatedList)
        }
    }

    fun moveGroupUp(group: GroupEntity) {
        val list = groups.value.toMutableList()
        val index = list.indexOfFirst { it.groupId == group.groupId }
        if (index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            updateGroupsOrder(list)
        }
    }

    fun moveGroupDown(group: GroupEntity) {
        val list = groups.value.toMutableList()
        val index = list.indexOfFirst { it.groupId == group.groupId }
        if (index >= 0 && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            updateGroupsOrder(list)
        }
    }

    fun getAppsForGroup(groupId: Int): Flow<List<AppEntity>> {
        return dao.getAppsByGroup(groupId)
    }

    fun toggleAppInGroup(groupId: Int, app: AppEntity, isIncluded: Boolean) {
        viewModelScope.launch {
            val crossRef = GroupAppCrossRef(groupId, app.packageName)
            if (isIncluded) {
                dao.insertGroupAppCrossRef(crossRef)
            } else {
                dao.deleteGroupAppCrossRef(crossRef)
            }
        }
    }

    init {
        syncApps()
    }

    fun onAppLaunched(packageName: String, onShouldFinish: () -> Unit) {
        viewModelScope.launch {
            val shouldExit = settingsRepository.exitOnLaunch.first()

            application.packageManager.getLaunchIntentForPackage(packageName)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application.startActivity(it)
                delay(1000)
                dao.updateLastLaunched(packageName, System.currentTimeMillis())

                if (shouldExit) { onShouldFinish() }
            }
        }
    }

    fun setAppVisibility(app: AppEntity, isVisible: Boolean) {
        viewModelScope.launch {
            // チェックON(=表示) なら isHidden=false
            // チェックOFF(=非表示) なら isHidden=true
            dao.updateHiddenState(app.packageName, isHidden = !isVisible)
        }
    }

    fun syncApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val myPackageName = context.packageName

            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val systemAppsMap = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != myPackageName }
                .associate {
                   it.activityInfo.packageName to it.loadLabel(pm).toString()
                }
            val systemPackageNames = systemAppsMap.keys

            val currentDbApps = dao.getAllApps().first()
            val dbPackageNames = currentDbApps.map { it.packageName }.toSet()

            val packagesToDelete = dbPackageNames - systemPackageNames
            if (packagesToDelete.isNotEmpty()) {
                dao.deleteApps(packagesToDelete.toList())
            }

            val packagesToAdd = systemPackageNames - dbPackageNames
            if (packagesToAdd.isNotEmpty()) {
                val newEntities = packagesToAdd.map { packageName ->
                    AppEntity(
                        packageName = packageName,
                        label = systemAppsMap[packageName] ?: "",
                        isHidden = false
                    )
                }
                dao.insertNewApps(newEntities)
            }

            currentDbApps.forEach { dbApp ->
                val systemLabel = systemAppsMap[dbApp.packageName]
                if (systemLabel != null && dbApp.label != systemLabel) {
                    dao.updateAppLabel(dbApp.packageName, systemLabel)
                }
            }
        }
    }
}