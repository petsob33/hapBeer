package cz.sobtech.hapBeer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beers")
data class BeerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val count: Int = 0
)
