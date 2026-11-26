package com.example.warehousescan.data

import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.data.auth.ApiAuthRepository
import com.example.warehousescan.data.auth.FakeAuthRepository
import com.example.warehousescan.data.auth.LocalAuthRepository
import com.example.warehousescan.data.auth.PasswordVerifier
import com.example.warehousescan.data.auth.PlainPasswordVerifier
import com.example.warehousescan.data.email.ApiEmailSender
import com.example.warehousescan.data.email.FakeEmailSender
import com.example.warehousescan.data.email.IntentEmailSender
import com.example.warehousescan.data.email.SaveOnlyEmailSender
import com.example.warehousescan.data.email.SmtpEmailSender
import com.example.warehousescan.data.scanner.DataWedgeScannerController
import com.example.warehousescan.data.scanner.FakeScannerController
import com.example.warehousescan.domain.repository.AuthRepository
import com.example.warehousescan.domain.repository.EmailSender
import com.example.warehousescan.domain.repository.ScannerController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePasswordVerifier(): PasswordVerifier = PlainPasswordVerifier()

    @Provides
    @Singleton
    fun provideAuthRepository(
        configProvider: ConfigProvider,
        localRepo: LocalAuthRepository,
        apiRepo: ApiAuthRepository,
        fakeRepo: FakeAuthRepository
    ): AuthRepository {
        return when (configProvider.config.value.providers.auth) {
            "api" -> apiRepo
            "fake" -> fakeRepo
            else -> localRepo
        }
    }

    @Provides
    @Singleton
    fun provideEmailSender(
        configProvider: ConfigProvider,
        apiSender: ApiEmailSender,
        intentSender: IntentEmailSender,
        saveOnlySender: SaveOnlyEmailSender,
        smtpSender: SmtpEmailSender,
        fakeSender: FakeEmailSender
    ): EmailSender {
        return when (configProvider.config.value.providers.email) {
            "api" -> apiSender
            "fake" -> fakeSender
            "smtp" -> smtpSender
            "saveonly" -> saveOnlySender
            else -> intentSender
        }
    }

    @Provides
    @Singleton
    fun provideScannerController(
        configProvider: ConfigProvider,
        dataWedge: DataWedgeScannerController,
        fake: FakeScannerController
    ): ScannerController {
        return when (configProvider.config.value.providers.scanner) {
            "fake" -> fake
            else -> dataWedge
        }
    }
}
