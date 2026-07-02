package cz.sobtech.hapBeer.data.repository

import cz.sobtech.hapBeer.data.dao.BeerRecordDao
import cz.sobtech.hapBeer.data.dao.EventDao
import cz.sobtech.hapBeer.data.dao.KegDao
import cz.sobtech.hapBeer.data.dao.PersonDao
import cz.sobtech.hapBeer.data.entity.BeerRecordEntity
import cz.sobtech.hapBeer.data.entity.EventEntity
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PivoRepository(
    private val eventDao: EventDao,
    private val personDao: PersonDao,
    private val kegDao: KegDao,
    private val beerRecordDao: BeerRecordDao
) {

    // ── Events ────────────────────────────────────────────────────────────────

    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getEvent(id: Long): Flow<EventEntity?> = eventDao.getEventById(id)

    suspend fun insertEvent(event: EventEntity): Long = eventDao.insert(event)

    suspend fun updateEvent(event: EventEntity) = eventDao.update(event)

    suspend fun deleteEvent(event: EventEntity) = eventDao.delete(event)

    // ── People (globální seznam) ───────────────────────────────────────────────

    val allPeople: Flow<List<PersonEntity>> = personDao.getAllPeople()

    suspend fun insertPerson(person: PersonEntity): Long = personDao.insert(person)

    suspend fun updatePerson(person: PersonEntity) = personDao.update(person)

    suspend fun deletePerson(person: PersonEntity) = personDao.delete(person)

    suspend fun findPersonByName(name: String): PersonEntity? = personDao.findByName(name)

    // ── Kegs ──────────────────────────────────────────────────────────────────

    fun getKeg(id: Long): Flow<KegEntity?> = kegDao.getKegById(id)

    fun getKegsForEvent(eventId: Long): Flow<List<KegEntity>> = kegDao.getKegsForEvent(eventId)

    suspend fun insertKeg(keg: KegEntity): Long = kegDao.insert(keg)

    suspend fun updateKeg(keg: KegEntity) = kegDao.update(keg)

    suspend fun deleteKeg(keg: KegEntity) = kegDao.delete(keg)

    // ── Beer records ──────────────────────────────────────────────────────────

    fun getRecordsForKeg(kegId: Long): Flow<List<BeerRecordEntity>> =
        beerRecordDao.getRecordsForKeg(kegId)

    /** personId → počet piv z konkrétní bečky. */
    fun getBeerCountsForKeg(kegId: Long): Flow<Map<Long, Int>> =
        beerRecordDao.getRecordsForKeg(kegId)
            .map { records -> records.groupingBy { it.personId }.eachCount() }

    fun getRecordsForEvent(eventId: Long): Flow<List<BeerRecordEntity>> =
        beerRecordDao.getRecordsForEvent(eventId)

    /** personId → celkový počet piv v rámci akce (přes všechny bečky). */
    fun getBeerCountsForEvent(eventId: Long): Flow<Map<Long, Int>> =
        beerRecordDao.getRecordsForEvent(eventId)
            .map { records -> records.groupingBy { it.personId }.eachCount() }

    suspend fun insertBeerRecord(record: BeerRecordEntity): Long = beerRecordDao.insert(record)

    suspend fun deleteBeerRecord(record: BeerRecordEntity) = beerRecordDao.delete(record)

    suspend fun deleteLastBeerRecordForPersonInKeg(kegId: Long, personId: Long) =
        beerRecordDao.deleteLastRecordForPersonInKeg(kegId, personId)
}
