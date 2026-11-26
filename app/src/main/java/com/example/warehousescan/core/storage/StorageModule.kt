package com.example.warehousescan.core.storage

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideWarehouseDatabase(
        @ApplicationContext context: Context
    ): WarehouseDatabase {
        val db = Room.databaseBuilder(
            context,
            WarehouseDatabase::class.java,
            "warehouse_scan_db"
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seeding handled in dao check
                }
            })
            .build()

        // Ensure seeded on startup if empty
        CoroutineScope(Dispatchers.IO).launch {
            seedUsersIfEmpty(context, db.userDao())
        }

        return db
    }

    @Provides
    fun provideLocationSessionDao(database: WarehouseDatabase): LocationSessionDao {
        return database.locationSessionDao()
    }

    @Provides
    fun provideLocationChallanDao(database: WarehouseDatabase): LocationChallanDao {
        return database.locationChallanDao()
    }

    @Provides
    fun provideVerificationSessionDao(database: WarehouseDatabase): VerificationSessionDao {
        return database.verificationSessionDao()
    }

    @Provides
    fun provideUserDao(database: WarehouseDatabase): UserDao {
        return database.userDao()
    }

    private suspend fun seedUsersIfEmpty(context: Context, userDao: UserDao) {
        try {
            if (userDao.getUserCount() == 0) {
                val inputStream = context.assets.open("config/users.json")
                val text = inputStream.bufferedReader().use { it.readText() }
                val json = Json { ignoreUnknownKeys = true }

                @Serializable
                data class SeedUserDto(
                    val userId: String,
                    val password: String,
                    val displayName: String,
                    val email: String,
                    val role: String,
                    val active: Boolean
                )
                @Serializable
                data class SeedDto(val users: List<SeedUserDto>)

                val seed = json.decodeFromString<SeedDto>(text)
                for (u in seed.users) {
                    userDao.insertUser(
                        UserEntity(
                            userId = u.userId,
                            password = u.password,
                            displayName = u.displayName,
                            email = u.email,
                            role = u.role,
                            active = u.active
                        )
                    )
                }
                Timber.i("Successfully seeded ${seed.users.size} users from users.json")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to seed users from users.json")
        }
    }
}
