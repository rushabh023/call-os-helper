package com.example.helper_application.dialer.core.utils

object T9Mapper {
    private val digitToLetters = mapOf(
        '2' to "ABC",
        '3' to "DEF",
        '4' to "GHI",
        '5' to "JKL",
        '6' to "MNO",
        '7' to "PQRS",
        '8' to "TUV",
        '9' to "WXYZ"
    )

    fun digitsToLetterPattern(digits: String): String =
        digits.map { digitToLetters[it]?.firstOrNull() ?: ' ' }.joinToString("")

    fun nameMatchesT9(name: String, t9Pattern: String): Boolean {
        if (t9Pattern.isBlank()) return false
        val letters = name.uppercase().filter { it.isLetter() }
        if (letters.length < t9Pattern.length) return false
        for (i in 0..letters.length - t9Pattern.length) {
            var matched = true
            for (j in t9Pattern.indices) {
                val ch = t9Pattern[j]
                if (ch == ' ') continue
                val letter = letters[i + j]
                val digit = digitToLetters.entries.firstOrNull { it.value.contains(letter) }?.key
                val expected = digitToLetters[digit ?: '0']?.firstOrNull()
                if (expected != ch) {
                    matched = false
                    break
                }
            }
            if (matched) return true
        }
        return false
    }
}
