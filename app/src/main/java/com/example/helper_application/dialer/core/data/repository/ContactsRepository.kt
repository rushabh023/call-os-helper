package com.example.helper_application.dialer.core.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.helper_application.dialer.core.utils.DialerTabPermissions
import com.example.helper_application.dialer.core.utils.PhoneNumberFormatter
import com.example.helper_application.dialer.feature.contacts.ContactRow
import com.example.helper_application.util.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun canRead(): Boolean = DialerTabPermissions.hasContactsPermission(context)

    fun load(query: String = "", limit: Int = 500): List<ContactRow> {
        if (!canRead()) return emptyList()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        val selection = if (query.isBlank()) null else
            "(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR " +
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?)"
        val args = if (query.isBlank()) null else arrayOf("%$query%", "%$query%")
        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                args,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val seen = LinkedHashSet<String>()
                buildList {
                    while (cursor.moveToNext() && size < limit) {
                        val id = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx).orEmpty().trim()
                        val rawNumber = cursor.getString(numIdx).orEmpty().trim()
                        if (name.isBlank() || rawNumber.isBlank()) continue
                        val normalized = PhoneNumberFormatter.normalizeForCompare(rawNumber)
                        val key = "$id:$normalized"
                        if (!seen.add(key)) continue
                        add(
                            ContactRow(
                                id = id,
                                name = name,
                                number = rawNumber,
                                displayNumber = PhoneNumberFormatter.formatForDisplay(rawNumber),
                                photoUri = cursor.getString(photoIdx)
                            )
                        )
                    }
                }
            }.orEmpty()
        } catch (e: SecurityException) {
            AppLog.w("Contacts read denied: ${e.message}")
            emptyList()
        }
    }
}
