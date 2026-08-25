package com.twotv.app.data.local

import android.content.Context
import androidx.room.*
import com.twotv.app.data.model.MediaType
import com.twotv.app.data.model.PairedTv
import com.twotv.app.data.model.SendHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedTvDao {
    @Query("SELECT * FROM paired_tvs ORDER BY lastSeenAt DESC")
    fun getAllTvs(): Flow<List<PairedTv>>

    @Query("SELECT * FROM paired_tvs WHERE isSelected = 1 LIMIT 1")
    fun getSelectedTvFlow(): Flow<PairedTv?>

    @Query("SELECT * FROM paired_tvs WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedTv(): PairedTv?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTv(tv: PairedTv)

    @Query("UPDATE paired_tvs SET isSelected = 0")
    suspend fun clearSelection()

    @Transaction
    suspend fun selectTv(tvId: String) {
        clearSelection()
        setSelectedInternal(tvId)
    }

    @Query("UPDATE paired_tvs SET isSelected = 1 WHERE id = :tvId")
    suspend fun setSelectedInternal(tvId: String)

    @Delete
    suspend fun deleteTv(tv: PairedTv)
}

@Dao
interface SendHistoryDao {
    @Query("SELECT * FROM send_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<SendHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: SendHistory)

    @Query("DELETE FROM send_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Long)

    @Query("DELETE FROM send_history")
    suspend fun clearHistory()
}

class Converters {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = try {
        MediaType.valueOf(value)
    } catch (e: Exception) {
        MediaType.VIDEO
    }
}

@Database(entities = [PairedTv::class, SendHistory::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pairedTvDao(): PairedTvDao
    abstract fun sendHistoryDao(): SendHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "twotv_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

