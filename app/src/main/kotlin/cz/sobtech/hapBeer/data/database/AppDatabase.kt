package cz.sobtech.hapBeer.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.sobtech.hapBeer.data.dao.BeerRecordDao
import cz.sobtech.hapBeer.data.dao.EventDao
import cz.sobtech.hapBeer.data.dao.KegDao
import cz.sobtech.hapBeer.data.dao.PersonDao
import cz.sobtech.hapBeer.data.entity.BeerRecordEntity
import cz.sobtech.hapBeer.data.entity.EventEntity
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity

@Database(
    entities = [
        EventEntity::class,
        PersonEntity::class,
        KegEntity::class,
        BeerRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun personDao(): PersonDao
    abstract fun kegDao(): KegDao
    abstract fun beerRecordDao(): BeerRecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Migrace 1→2: drop event_person, přebudování beer_records bez eventId. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `event_person`")
                db.execSQL("DROP TABLE IF EXISTS `beer_records`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `beer_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kegId` INTEGER NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`kegId`) REFERENCES `kegs`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`personId`) REFERENCES `people`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_beer_records_kegId` ON `beer_records` (`kegId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_beer_records_personId` ON `beer_records` (`personId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hapbeer.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys=ON")
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
