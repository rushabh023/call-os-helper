package com.example.helper_application.shizuku;

interface IShizukuRecordingService {
    /** Required by Shizuku server — transaction id is fixed. */
    void destroy() = 16777114;

    /** @return true if recording started */
    boolean startRecording(String outputPath) = 1;
    /** @return absolute path of saved file, or empty on failure */
    String stopRecording() = 2;
    /** @return last audio source label used (e.g. VOICE_CALL) */
    String getLastAudioSource() = 3;
    boolean isRecording() = 4;
}
