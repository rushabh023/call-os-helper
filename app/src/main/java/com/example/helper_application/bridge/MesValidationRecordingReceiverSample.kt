package com.example.helper_application.bridge

/**
 * COPY INTO Mes Validation app (com.mesvalidation) — NOT into Helper Application.
 *
 * AndroidManifest.xml inside Mes Validation:
 * <receiver
 *     android:name=".recording.MesValidationRecordingReceiver"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.mesvalidation.action.START_RECORD" />
 *         <action android:name="com.mesvalidation.action.STOP_RECORD" />
 *     </intent-filter>
 * </receiver>
 */
/*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MesValidationRecordingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "com.mesvalidation.action.START_RECORD" -> {
                // TODO: start recording in Mes Validation
            }
            "com.mesvalidation.action.STOP_RECORD" -> {
                // TODO: stop and save recording in Mes Validation
            }
        }
    }
}
*/
