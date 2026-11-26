package com.example.warehousescan.data.email

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.example.warehousescan.domain.repository.EmailSender
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveOnlyEmailSender @Inject constructor(
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
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: return Result.failure(IllegalStateException("Failed to create MediaStore entry"))

            val outputStream: OutputStream = resolver.openOutputStream(uri)
                ?: return Result.failure(IllegalStateException("Failed to open output stream"))

            outputStream.use {
                it.write(fileBytes)
            }

            Timber.i("Saved TXT file to Downloads: $fileName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save file to Downloads")
            Result.failure(e)
        }
    }
}
