package com.example.helper_application.dialer.core.utils

object PhoneNumberFormatter {
    fun sanitizeForDial(input: String): String =
        input.filter { it.isDigit() || it == '*' || it == '#' || it == '+' }

    fun normalizeForCompare(number: String): String =
        number.filter { it.isDigit() || it == '+' }

    fun formatForDisplay(number: String): String {
        val digits = number.filter { it.isDigit() }
        if (digits.length == 10) {
            return "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        }
        return number.trim()
    }

    fun displayNameForFile(contactName: String?, phoneNumber: String?): String {
        val base = contactName?.takeIf { it.isNotBlank() }
            ?: phoneNumber?.filter { it.isDigit() || it == '+' }
            ?: "Unknown"
        return base.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(48)
    }
}
