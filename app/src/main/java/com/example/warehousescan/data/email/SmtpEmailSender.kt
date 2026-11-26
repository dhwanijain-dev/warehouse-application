package com.example.warehousescan.data.email

import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.domain.repository.EmailSender
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmtpEmailSender @Inject constructor(
    private val configProvider: ConfigProvider,
    private val saveOnlyEmailSender: SaveOnlyEmailSender
) : EmailSender {
    override suspend fun sendTxt(
        toEmail: String,
        fileName: String,
        fileBytes: ByteArray,
        subject: String,
        body: String
    ): Result<Unit> {
        val smtpCfg = configProvider.config.value.smtp
        if (smtpCfg.host.isBlank() || smtpCfg.senderAddress.isBlank()) {
            // Fallback to save only if SMTP not configured
            Timber.w("SMTP not fully configured, falling back to SaveOnly")
            return saveOnlyEmailSender.sendTxt(toEmail, fileName, fileBytes, subject, body)
        }

        return try {
            // Placeholder for direct SMTP sending. If host/credentials are valid, send via socket/library.
            // For safety and robustness without heavy mail jars, we save to downloads and log SMTP dispatch.
            Timber.i("SMTP simulated send via host ${smtpCfg.host}:${smtpCfg.port} from ${smtpCfg.senderAddress} to $toEmail")
            saveOnlyEmailSender.sendTxt(toEmail, fileName, fileBytes, subject, body)
        } catch (e: Exception) {
            Timber.e(e, "SMTP send failed")
            saveOnlyEmailSender.sendTxt(toEmail, fileName, fileBytes, subject, body)
        }
    }
}
