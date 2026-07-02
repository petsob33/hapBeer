package cz.sobtech.hapBeer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cz.sobtech.hapBeer.data.entity.KegEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KegDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keg: KegEntity): Long

    @Update
    suspend fun update(keg: KegEntity)

    @Delete
    suspend fun delete(keg: KegEntity)

    @Query("SELECT * FROM kegs ORDER BY name ASC")
    fun getAllKegs(): Flow<List<KegEntity>>

    @Query("SELECT * FROM kegs WHERE eventId = :eventId ORDER BY name ASC")
    fun getKegsForEvent(eventId: Long): Flow<List<KegEntity>>

    @Query("SELECT * FROM kegs WHERE id = :id")
    fun getKegById(id: Long): Flow<KegEntity?>
}
