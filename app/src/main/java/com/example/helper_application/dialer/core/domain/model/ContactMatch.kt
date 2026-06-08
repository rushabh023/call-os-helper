package com.example.helper_application.dialer.core.domain.model

import android.net.Uri

data class ContactMatch(
    val contactId: Long,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: Uri? = null,
    val matchSource: MatchSource
)

enum class MatchSource {
    CONTACT,
    CALL_LOG
}
