package cz.sobtech.hapBeer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cz.sobtech.hapBeer.data.entity.BeerRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BeerRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(record: BeerRecordEntity): Long

    @Delete
    abstract suspend fun delete(record: BeerRecordEntity)

    /** Záznamy pro konkrétní bečku – hlavní query pro KegDetailScreen. */
    @Query("SELECT * FROM beer_records WHERE kegId = :kegId ORDER BY timestamp DESC")
    abstract fun getRecordsForKeg(kegId: Long): Flow<List<BeerRecordEntity>>

    /** Záznamy pro akci odvozené přes bečky – pro EventSummaryBottomSheet. */
    @Query("SELECT * FROM beer_records WHERE kegId IN (SELECT id FROM kegs WHERE eventId = :eventId)")
    abstract fun getRecordsForEvent(eventId: Long): Flow<List<BeerRecordEntity>>

    @Query("""
        SELECT * FROM beer_records
        WHERE kegId = :kegId AND personId = :personId
        ORDER BY timestamp DESC LIMIT 1
    """)
    abstract suspend fun getLastRecordForPersonInKeg(kegId: Long, personId: Long): BeerRecordEntity?

    @Query("DELETE FROM beer_records WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Transaction
    open suspend fun deleteLastRecordForPersonInKeg(kegId: Long, personId: Long) {
        val record = getLastRecordForPersonInKeg(kegId, personId)
        if (record != null) deleteById(record.id)
    }
}
