package com.example.warehousescan.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_session")
data class LocationSessionEntity(
    @PrimaryKey val id: Int = 1,
    val rack: String,
    val zone: String,
    val setting: String,
    val batchesJson: String // JSON list of strings
)

@Entity(
    tableName = "location_challans",
    indices = [androidx.room.Index(value = ["rack", "zone", "setting"], unique = true)]
)
data class LocationChallanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rack: String,
    val zone: String,
    val setting: String,
    val batchesJson: String,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "verification_session")
data class VerificationSessionEntity(
    @PrimaryKey val id: Int = 1,
    val uploadedJson: String,
    val matchedJson: String,
    val unmatchedJson: String,
    val attemptCount: Int
)
