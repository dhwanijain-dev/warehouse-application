package com.example.warehousescan.data.email

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.core.network.GenericApi
import com.example.warehousescan.core.network.safeApiCall
import com.example.warehousescan.domain.repository.EmailSender
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiEmailSender @Inject constructor(
    private val api: GenericApi,
    private val configProvider: ConfigProvider
) : EmailSender {
    override suspend fun sendTxt(
        toEmail: String,
        fileName: String,
        fileBytes: ByteArray,
        subject: String,
        body: String
    ): Result<Unit> {
        val endpoint = configProvider.config.value.api.endpoints.sendTxtEmail
        val url = endpoint.path

        val fileRequestBody = fileBytes.toRequestBody("text/plain".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", fileName, fileRequestBody)

        val emailPart = MultipartBody.Part.createFormData("email", toEmail)
        val subjectPart = MultipartBody.Part.createFormData("subject", subject)
        val bodyPart = MultipartBody.Part.createFormData("body", body)

        val result = safeApiCall {
            api.postMultipart(url, listOf(filePart, emailPart, subjectPart, bodyPart))
        }

        return result.map { }
    }
}

@Singleton
class IntentEmailSender @Inject constructor(
    @ApplicationContext private val context: Context
) : EmailSender {
    override suspend fun sendTxt(
        toEmail: String,
        fileName: String,
        fileBytes: ByteArray,
        subject: String,
        body: String
    ): Result<Unit> {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeBytes(fileBytes)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Send email via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send email via intent")
            Result.failure(e)
        }
    }
}

@Singleton
class FakeEmailSender @Inject constructor() : EmailSender {
    override suspend fun sendTxt(
        toEmail: String,
        fileName: String,
        fileBytes: ByteArray,
        subject: String,
        body: String
    ): Result<Unit> {
        kotlinx.coroutines.delay(300)
        Timber.i("FakeEmailSender sent email to $toEmail with file $fileName (subject: $subject)")
        return Result.success(Unit)
    }
}
