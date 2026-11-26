package com.example.warehousescan.domain.repository

interface EmailSender {
    suspend fun sendTxt(
        toEmail: String,
        fileName: String,
        fileBytes: ByteArray,
        subject: String,
        body: String
    ): Result<Unit>
}
