package jp.deadend.noname.llauncher

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String, // IDはパッケージ名
    val label: String,
    val isHidden: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val lastLaunched: Long = 0
)

@Entity(tableName = "groups_table")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val groupId: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0
)

@Entity(
    tableName = "group_app_cross_ref",
    primaryKeys = ["groupId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packageName")]
)

data class GroupAppCrossRef(
    val groupId: Int,
    val packageName: String
)
@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY label ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNewApps(apps: List<AppEntity>)

    @Query("DELETE FROM apps WHERE packageName IN (:packageList)")
    suspend fun deleteApps(packageList: List<String>)

    @Query("UPDATE apps SET label = :label WHERE packageName = :packageName")
    suspend fun updateAppLabel(packageName: String, label: String)

    @Query("UPDATE apps SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun updateHiddenState(packageName: String, isHidden: Boolean)

    @Query("UPDATE apps SET lastLaunched = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastLaunched(packageName: String, timestamp: Long)


    @Query("SELECT * FROM groups_table ORDER BY sortOrder ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupAppCrossRef(crossRef: GroupAppCrossRef)

    @Delete
    suspend fun deleteGroupAppCrossRef(crossRef: GroupAppCrossRef)

    @Update
    suspend fun updateGroups(groups: List<GroupEntity>)

    @Query(
        """
        SELECT apps.* FROM apps 
        INNER JOIN group_app_cross_ref ON apps.packageName = group_app_cross_ref.packageName
        WHERE group_app_cross_ref.groupId = :groupId 
        AND apps.isHidden = 0
    """
    )
    fun getAppsByGroup(groupId: Int): Flow<List<AppEntity>>
}

@Database(
    entities = [
        AppEntity::class,
        GroupEntity::class,
        GroupAppCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "app_launcher_db"
                            )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}