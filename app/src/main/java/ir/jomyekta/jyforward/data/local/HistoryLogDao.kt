package ir.jomyekta.jyforward.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryLogDao {

    @Query("SELECT * FROM history_logs ORDER BY timestamp DESC")
    fun observeAllLogs(): Flow<List<HistoryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HistoryLogEntity): Long

    @Query("DELETE FROM history_logs")
    suspend fun clearAllLogs()
}
