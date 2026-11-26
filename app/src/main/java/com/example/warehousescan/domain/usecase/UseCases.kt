package com.example.warehousescan.domain.usecase

import com.example.warehousescan.core.config.ConfigProvider
import com.example.warehousescan.domain.engine.TemplateRenderer
import com.example.warehousescan.domain.engine.TxtFileGenerator
import com.example.warehousescan.domain.model.Location
import com.example.warehousescan.domain.model.User
import com.example.warehousescan.domain.repository.AuthRepository
import com.example.warehousescan.domain.repository.EmailSender
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, pass: String): Result<User> {
        if (username.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password are required"))
        }
        return authRepository.login(username, pass)
    }
}

class SubmitLocationUseCase @Inject constructor(
    private val configProvider: ConfigProvider,
    private val emailSender: EmailSender
) {
    suspend operator fun invoke(
        user: User,
        location: Location,
        batches: List<String>
    ): Result<Unit> {
        val cfg = configProvider.config.value
        val txtContent = TxtFileGenerator.generateContent(location, batches, cfg.txtFile)
        val fileBytes = txtContent.toByteArray(Charsets.UTF_8)

        val rawFileName = cfg.txtFile.fileNamePattern
            .replace("{rack}", location.rack)
            .replace("{zone}", location.zone)
            .replace("{setting}", location.setting)
        val fileName = TemplateRenderer.sanitizeFilename(rawFileName)

        val subject = TemplateRenderer.render(
            template = cfg.email.subjectTemplate,
            rack = location.rack,
            zone = location.zone,
            setting = location.setting,
            user = user.username,
            count = batches.size
        )

        val body = TemplateRenderer.render(
            template = cfg.email.bodyTemplate,
            rack = location.rack,
            zone = location.zone,
            setting = location.setting,
            user = user.username,
            count = batches.size
        )

        val emailTo = user.email.ifBlank { "user@warehouse.local" }

        return emailSender.sendTxt(
            toEmail = emailTo,
            fileName = fileName,
            fileBytes = fileBytes,
            subject = subject,
            body = body
        )
    }
}
