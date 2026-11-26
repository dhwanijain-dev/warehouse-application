package com.example.warehousescan.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationSessionEntity::class, LocationChallanEntity::class, VerificationSessionEntity::class, UserEntity::class],
    version = 3,
    exportSchema = false
)
abstract class WarehouseDatabase : RoomDatabase() {
    abstract fun locationSessionDao(): LocationSessionDao
    abstract fun locationChallanDao(): LocationChallanDao
    abstract fun verificationSessionDao(): VerificationSessionDao
    abstract fun userDao(): UserDao
}
