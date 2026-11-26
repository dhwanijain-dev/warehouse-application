package com.example.warehousescan.data.auth

interface PasswordVerifier {
    fun verify(rawPassword: String, storedPassword: String): Boolean
}

class PlainPasswordVerifier : PasswordVerifier {
    override fun verify(rawPassword: String, storedPassword: String): Boolean {
        return rawPassword == storedPassword
    }
}
