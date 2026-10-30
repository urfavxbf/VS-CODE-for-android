package dev.codespacemobile.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "codespaces")
data class CodespaceEntity(
    @PrimaryKey val name: String,
    val title: String,
    val repoName: String,
    val state: String,
    val webUrl: String?,
    val updatedAt: String?
)

@Dao
abstract class CodespaceDao {
    @Query("SELECT * FROM codespaces ORDER BY title COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<CodespaceEntity>>

    @Query("DELETE FROM codespaces")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<CodespaceEntity>)

    @Transaction
    open suspend fun replaceAll(items: List<CodespaceEntity>) {
        clear()
        insertAll(items)
    }
}

@Database(entities = [CodespaceEntity::class], version = 1, exportSchema = true)
abstract class CodespaceDatabase : RoomDatabase() {
    abstract fun dao(): CodespaceDao
}
