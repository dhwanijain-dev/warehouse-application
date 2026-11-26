package com.example.warehousescan.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationSessionDao {
    @Query("SELECT * FROM location_session WHERE id = 1")
    fun getLocationSession(): Flow<LocationSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocationSession(session: LocationSessionEntity)

    @Query("DELETE FROM location_session WHERE id = 1")
    suspend fun clearLocationSession()
}

@Dao
interface LocationChallanDao {
    @Query("SELECT * FROM location_challans WHERE status = 'PENDING' ORDER BY updatedAt DESC")
    fun getPendingChallans(): Flow<List<LocationChallanEntity>>

    @Query("SELECT * FROM location_challans WHERE LOWER(rack) = LOWER(:rack) AND LOWER(zone) = LOWER(:zone) AND LOWER(setting) = LOWER(:setting) LIMIT 1")
    suspend fun getChallanByLocation(rack: String, zone: String, setting: String): LocationChallanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChallan(challan: LocationChallanEntity): Long

    @Query("DELETE FROM location_challans WHERE id = :id")
    suspend fun deleteChallan(id: Long)
}

@Dao
interface VerificationSessionDao {
    @Query("SELECT * FROM verification_session WHERE id = 1")
    fun getVerificationSession(): Flow<VerificationSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVerificationSession(session: VerificationSessionEntity)

    @Query("DELETE FROM verification_session WHERE id = 1")
    suspend fun clearVerificationSession()
}
