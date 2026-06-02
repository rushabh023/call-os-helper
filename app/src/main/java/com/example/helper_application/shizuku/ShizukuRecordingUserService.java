package com.example.helper_application.shizuku;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;

/**
 * Shizuku user service — Java (not Kotlin) for reliable reflection from
 * {@code rikka.shizuku.server.UserService} in the shell process.
 */
public class ShizukuRecordingUserService extends IShizukuRecordingService.Stub {

    private static final String TAG = "HelperApplication";

    private final Context serviceContext;
    private MediaRecorder mediaRecorder;
    private String outputPath;
    private String lastSourceLabel = "unknown";

    /** Fallback for older Shizuku loaders; API v13+ uses [ShizukuRecordingUserService#ShizukuRecordingUserService]. */
    @Keep
    public ShizukuRecordingUserService() {
        serviceContext = null;
        Log.i(TAG, "[Shizuku] user_service_created | ctor=no-arg pid=" + Process.myPid());
    }

    @Keep
    public ShizukuRecordingUserService(Context context) {
        serviceContext = context.getApplicationContext();
        Log.i(TAG, "[Shizuku] user_service_created | ctor=context pid=" + Process.myPid()
                + " sdk=" + Build.VERSION.SDK_INT
                + " package=" + serviceContext.getPackageName());
    }

    @Override
    public void destroy() {
        Log.i(TAG, "[Shizuku] user_service_destroy pid=" + Process.myPid());
        releaseRecorder();
        System.exit(0);
    }

    @Override
    public boolean startRecording(String path) {
        if (serviceContext == null) {
            Log.e(TAG, "[Shizuku] startRecording: no Context (wrong constructor?)");
            return false;
        }
        if (mediaRecorder != null) {
            Log.d(TAG, "[Shizuku] startRecording: already active");
            return true;
        }
        lastSourceLabel = "unknown";
        File file = new File(path);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }

        int[] sources;
        String[] labels;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sources = new int[]{
                    MediaRecorder.AudioSource.VOICE_CALL,
                    MediaRecorder.AudioSource.VOICE_DOWNLINK,
                    MediaRecorder.AudioSource.VOICE_UPLINK,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.VOICE_PERFORMANCE,
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.DEFAULT,
            };
            labels = new String[]{
                    "VOICE_CALL", "VOICE_DOWNLINK", "VOICE_UPLINK", "VOICE_COMMUNICATION",
                    "VOICE_RECOGNITION", "VOICE_PERFORMANCE", "MIC", "DEFAULT",
            };
        } else {
            sources = new int[]{
                    MediaRecorder.AudioSource.VOICE_CALL,
                    MediaRecorder.AudioSource.VOICE_DOWNLINK,
                    MediaRecorder.AudioSource.VOICE_UPLINK,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.DEFAULT,
            };
            labels = new String[]{
                    "VOICE_CALL", "VOICE_DOWNLINK", "VOICE_UPLINK", "VOICE_COMMUNICATION",
                    "VOICE_RECOGNITION", "MIC", "DEFAULT",
            };
        }

        Log.i(TAG, "[Shizuku] shell recording start — trying " + sources.length + " sources");
        for (int i = 0; i < sources.length; i++) {
            releaseRecorder();
            if (tryStart(file, sources[i], labels[i])) {
                outputPath = path;
                lastSourceLabel = labels[i];
                Log.i(TAG, "[Shizuku] shell_recording_started source=" + lastSourceLabel
                        + " path=" + path);
                return true;
            }
        }
        Log.e(TAG, "[Shizuku] shell recording failed: all sources rejected");
        return false;
    }

    private boolean tryStart(File file, int source, String label) {
        try {
            MediaRecorder recorder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(serviceContext);
            } else {
                recorder = new MediaRecorder();
            }
            recorder.setAudioSource(source);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128_000);
            recorder.setAudioSamplingRate(44_100);
            recorder.setOutputFile(file.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            mediaRecorder = recorder;
            return true;
        } catch (Exception e) {
            Log.w(TAG, "[Shizuku] source_failed label=" + label + " error=" + e.getMessage());
            return false;
        }
    }

    @Override
    public String stopRecording() {
        String path = outputPath;
        try {
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Log.w(TAG, "[Shizuku] MediaRecorder.stop: " + e.getMessage());
                }
                mediaRecorder.release();
            }
        } finally {
            mediaRecorder = null;
            outputPath = null;
        }
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.length() > 0L) {
                Log.i(TAG, "[Shizuku] shell_recording_stopped source=" + lastSourceLabel
                        + " bytes=" + file.length());
                return path;
            }
        }
        Log.w(TAG, "[Shizuku] shell stop: no usable file");
        return "";
    }

    @Override
    public String getLastAudioSource() {
        return lastSourceLabel;
    }

    @Override
    public boolean isRecording() {
        return mediaRecorder != null;
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception ignored) {
            }
            mediaRecorder = null;
        }
    }
}
