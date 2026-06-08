package com.example.dialer

import com.example.dialer.core.utils.T9Mapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchUseCaseTest {

    @Test
    fun digitsToLetterPattern_mapsDigitsToFirstKeyLetters() {
        assertTrue(T9Mapper.digitsToLetterPattern("555").contains("M"))
    }

    @Test
    fun t9_rejectsVeryShortPatternOnLongName() {
        assertFalse(T9Mapper.nameMatchesT9("Alice Wonderland", "Z"))
    }
}
