package com.example.dialer.feature.contacts

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactRow(val id: Long, val name: String, val number: String)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactRow>>(emptyList())
    val contacts: StateFlow<List<ContactRow>> = _contacts.asStateFlow()

    init { search("") }

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _contacts.value = load(query)
        }
    }

    private fun load(query: String): List<ContactRow> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = if (query.isBlank()) null else
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val args = if (query.isBlank()) null else arrayOf("%$query%", "%$query%")
        return context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            args,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { c ->
            val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            buildList {
                while (c.moveToNext()) {
                    add(
                        ContactRow(
                            id = c.getLong(idIdx),
                            name = c.getString(nameIdx).orEmpty(),
                            number = c.getString(numIdx).orEmpty()
                        )
                    )
                }
            }
        }.orEmpty()
    }
}
