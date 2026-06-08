package com.example.dialer.core.domain.usecase

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.dialer.core.domain.model.ContactMatch
import com.example.dialer.core.domain.model.MatchSource
import com.example.dialer.core.utils.T9Mapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContactSearchUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun search(digits: String, limit: Int = 25): List<ContactMatch> {
        if (digits.isBlank()) return emptyList()
        val normalizedDigits = digits.filter { it.isDigit() }
        val t9Letters = T9Mapper.digitsToLetterPattern(normalizedDigits)
        val results = LinkedHashMap<String, ContactMatch>()

        searchContacts(normalizedDigits, t9Letters, results, limit)
        searchCallLog(normalizedDigits, results, limit)

        return results.values.take(limit)
    }

    private fun searchContacts(
        digits: String,
        t9Letters: String,
        results: LinkedHashMap<String, ContactMatch>,
        limit: Int
    ) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (cursor.moveToNext() && results.size < limit) {
                val number = cursor.getString(numberIdx)?.filter { it.isDigit() } ?: continue
                val name = cursor.getString(nameIdx) ?: continue
                val matchesNumber = number.contains(digits)
                val matchesT9 = T9Mapper.nameMatchesT9(name, t9Letters)
                if (!matchesNumber && !matchesT9) continue
                val key = "${cursor.getLong(idIdx)}:$number"
                results[key] = ContactMatch(
                    contactId = cursor.getLong(idIdx),
                    displayName = name,
                    phoneNumber = cursor.getString(numberIdx) ?: number,
                    photoUri = cursor.getString(photoIdx)?.let { android.net.Uri.parse(it) },
                    matchSource = MatchSource.CONTACT
                )
            }
        }
    }

    private fun searchCallLog(
        digits: String,
        results: LinkedHashMap<String, ContactMatch>,
        limit: Int
    ) {
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME
        )
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            while (cursor.moveToNext() && results.size < limit) {
                val rawNumber = cursor.getString(numberIdx) ?: continue
                val number = rawNumber.filter { it.isDigit() }
                if (!number.contains(digits)) continue
                val key = "log:$rawNumber"
                if (results.containsKey(key)) continue
                results[key] = ContactMatch(
                    contactId = -1L,
                    displayName = cursor.getString(nameIdx) ?: rawNumber,
                    phoneNumber = rawNumber,
                    photoUri = null,
                    matchSource = MatchSource.CALL_LOG
                )
            }
        }
    }
}
