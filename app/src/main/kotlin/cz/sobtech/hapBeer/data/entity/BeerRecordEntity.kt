package cz.sobtech.hapBeer.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "beer_records",
    foreignKeys = [
        ForeignKey(
            entity = KegEntity::class,
            parentColumns = ["id"],
            childColumns = ["kegId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("kegId"), Index("personId")]
)
data class BeerRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kegId: Long,
    val personId: Long,
    val timestamp: Long
)
