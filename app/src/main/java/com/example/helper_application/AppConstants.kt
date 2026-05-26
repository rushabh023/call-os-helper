package com.example.helper_application

object AppConstants {

    /** Mes Validation main app — must match applicationId in Mes Validation's build.gradle */
    const val MES_VALIDATION_PACKAGE = "com.mesvalidation"

    /** @deprecated Use [MES_VALIDATION_PACKAGE] */
    const val ABC_MAIN_PACKAGE = MES_VALIDATION_PACKAGE
    const val MAIN_RECORDER_PACKAGE = MES_VALIDATION_PACKAGE

    /** Mes Validation must handle these in a BroadcastReceiver or Service */
    const val ACTION_START_RECORD = "$MES_VALIDATION_PACKAGE.action.START_RECORD"
    const val ACTION_STOP_RECORD = "$MES_VALIDATION_PACKAGE.action.STOP_RECORD"

    const val EXTRA_DIRECTION = "direction"
    const val EXTRA_PHONE_NUMBER = "phone_number"

    const val BUILD_VERSION = "1.0.0-mes-helper"
    const val SUPPORT_EMAIL = "support@example.com"
}
