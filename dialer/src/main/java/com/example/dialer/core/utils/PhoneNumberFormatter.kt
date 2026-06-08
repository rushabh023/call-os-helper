package com.example.dialer.core.utils

object PhoneNumberFormatter {
    fun sanitizeForDial(input: String): String =
        input.filter { it.isDigit() || it == '*' || it == '#' || it == '+' }

    fun displayNameForFile(contactName: String?, phoneNumber: String?): String {
        val base = contactName?.takeIf { it.isNotBlank() }
            ?: phoneNumber?.filter { it.isDigit() || it == '+' }
            ?: "Unknown"
        return base.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(48)
    }
}
