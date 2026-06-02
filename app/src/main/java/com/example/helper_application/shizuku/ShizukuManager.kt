package com.example.helper_application.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.helper_application.util.AppLog
import rikka.shizuku.Shizuku

/**
 * Binds [ShizukuRecordingUserService] in shell-privileged process (ACR-style two-way recording).
 *
 * Connection has two layers:
 * 1. Shizuku access — binder + permission (can work on Samsung)
 * 2. User service — separate subprocess; often fails on some OEM builds (not a "disconnect" of layer 1)
 */
object ShizukuManager {

    private const val PERMISSION_REQUEST_CODE = 9001
    /** Official demo uses a short suffix; keep stable across installs. */
    private const val PROCESS_NAME_SUFFIX = "service"
    private const val USER_SERVICE_VERSION = 1
    private const val BIND_TIMEOUT_MS = 20_000L
    private const val FIRST_BIND_DELAY_MS = 2_500L
    /** Exactly this many bind attempts per app session; then show ROM-blocked message (no manual retry). */
    private const val SERVICE_BLOCKED_AFTER_TIMEOUTS = 4

    private const val USER_SERVICE_CLASS =
        "com.example.helper_application.shizuku.ShizukuRecordingUserService"

    private val managerPackages = listOf(
        "moe.shizuku.privileged.api",
        "moe.shizuku.privileged.manager",
        "com.nll.shizuku.privileged.api"
    )

    const val PLAY_STORE_PACKAGE = "moe.shizuku.privileged.api"
    const val SETUP_GUIDE_URL = "https://shizuku.rikka.app/guide/setup.html"

    @Volatile
    private var recordingService: IShizukuRecordingService? = null

    @Volatile
    private var bindRequested = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var bindTimeoutRunnable: Runnable? = null

    @Volatile
    private var consecutiveBindTimeouts = 0

    /** One automatic 4-attempt cycle per session; no manual RETRY on Dashboard. */
    @Volatile
    private var bindCycleStarted = false

    @Volatile
    private var lastContext: Context? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        AppLog.Shizuku.i("Binder received from Shizuku manager")
        logStatus("binder_received")
        if (hasPermission() && !isUserServiceBlocked()) {
            mainHandler.postDelayed({
                bindUserServiceIfNeeded(lastContext)
            }, 1_000L)
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        AppLog.Shizuku.w(
            "Binder dead — reopen Shizuku app (Start). If you toggled Helper in Shizuku: force-stop Helper, open again."
        )
        cancelBindTimeout()
        recordingService = null
        bindRequested = false
        logStatus("binder_dead")
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            AppLog.Shizuku.detail(
                "permission_result",
                "requestCode" to requestCode,
                "grantResult" to grantResult,
                "granted" to granted
            )
            if (granted && !isUserServiceBlocked()) {
                bindUserServiceIfNeeded(lastContext)
            } else if (!granted) {
                AppLog.Shizuku.w("User denied Shizuku permission")
            }
            logStatus("after_permission_result")
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            cancelBindTimeout()
            val alive = binder != null && binder.pingBinder()
            recordingService = if (alive) {
                IShizukuRecordingService.Stub.asInterface(binder)
            } else {
                null
            }
            bindRequested = false
            consecutiveBindTimeouts = 0
            bindCycleStarted = true
            AppLog.Shizuku.detail(
                "user_service_connected",
                "component" to (name?.flattenToString() ?: "null"),
                "binderAlive" to alive,
                "recordingInterface" to (recordingService != null)
            )
            logStatus("service_connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cancelBindTimeout()
            recordingService = null
            bindRequested = false
            AppLog.Shizuku.w("User service disconnected: ${name?.flattenToString()}")
            logStatus("service_disconnected")
        }
    }

    fun init(context: Context) {
        lastContext = context.applicationContext
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }.onFailure { e ->
            AppLog.Shizuku.e("Failed to register Shizuku listeners", e)
            return
        }
        AppLog.Shizuku.i("ShizukuManager initialized")
        logStatus("init")
        if (isBinderAvailable() && hasPermission() && !isUserServiceBlocked()) {
            mainHandler.postDelayed({
                bindUserServiceIfNeeded(lastContext)
            }, FIRST_BIND_DELAY_MS)
        }
    }

    fun isShizukuInstalled(context: Context): Boolean =
        managerPackages.any { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }

    fun isBinderAvailable(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean {
        if (!isBinderAvailable()) return false
        if (Shizuku.isPreV11()) return false
        return runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    /** Binder + permission — Shizuku "allows" the app; does not mean recording subprocess works. */
    fun hasShizukuAccess(): Boolean = isBinderAvailable() && hasPermission()

    fun isReady(): Boolean = hasShizukuAccess() && recordingService != null

    fun isBindingInProgress(): Boolean = bindRequested && recordingService == null

    fun isUserServiceBlocked(): Boolean =
        consecutiveBindTimeouts >= SERVICE_BLOCKED_AFTER_TIMEOUTS && recordingService == null

    fun bindAttemptCount(): Int = consecutiveBindTimeouts

    fun maxBindAttemptsBeforeBlocked(): Int = SERVICE_BLOCKED_AFTER_TIMEOUTS

    fun readinessReason(): String {
        if (!isBinderAvailable()) return "binder_unavailable"
        if (!hasPermission()) return "permission_denied"
        if (isUserServiceBlocked()) return "service_bind_blocked"
        if (isBindingInProgress()) return "service_binding"
        if (recordingService == null) return "service_disconnected"
        return "ready"
    }

    fun requestPermission() {
        if (!isBinderAvailable()) {
            AppLog.Shizuku.w("Cannot request permission — start Shizuku first")
            return
        }
        if (Shizuku.isPreV11()) return
        when (runCatching { Shizuku.checkSelfPermission() }.getOrElse { PackageManager.PERMISSION_DENIED }) {
            PackageManager.PERMISSION_GRANTED -> bindUserServiceIfNeeded(lastContext)
            else -> runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
                .onFailure { e -> AppLog.Shizuku.e("requestPermission failed", e) }
        }
    }

    fun bindUserServiceIfNeeded(context: Context?) {
        val ctx = context?.applicationContext ?: lastContext
        if (ctx == null) return
        if (ShizukuProcess.isUserServiceProcess(ctx)) return
        lastContext = ctx
        if (!hasShizukuAccess()) {
            logStatus("bind_skipped_no_access")
            return
        }
        if (isUserServiceBlocked()) {
            AppLog.Shizuku.w(
                "bind skipped: ROM blocked Shizuku UserService after $consecutiveBindTimeouts/$SERVICE_BLOCKED_AFTER_TIMEOUTS attempts"
            )
            logStatus("bind_skipped_blocked")
            return
        }
        if (bindRequested || recordingService != null) return

        val component = userServiceComponent(ctx)
        if (tryAttachExistingUserService(component)) return

        if (consecutiveBindTimeouts > 0) {
            runCatching {
                Shizuku.unbindUserService(userServiceArgs(component), serviceConnection, true)
            }
        }

        bindRequested = true
        bindCycleStarted = true
        try {
            Shizuku.bindUserService(userServiceArgs(component), serviceConnection)
            AppLog.Shizuku.detail(
                "bind_requested",
                "component" to component.flattenToString(),
                "processSuffix" to PROCESS_NAME_SUFFIX,
                "daemon" to false,
                "debuggable" to false,
                "version" to USER_SERVICE_VERSION,
                "shizukuUid" to shizukuUid(),
                "attempt" to (consecutiveBindTimeouts + 1)
            )
            scheduleBindTimeout()
        } catch (e: Exception) {
            cancelBindTimeout()
            bindRequested = false
            AppLog.Shizuku.e("bindUserService failed", e)
            logStatus("bind_failed")
        }
    }

    private fun scheduleBindTimeout() {
        cancelBindTimeout()
        val runnable = Runnable {
            bindTimeoutRunnable = null
            if (!bindRequested || recordingService != null) return@Runnable
            bindRequested = false
            consecutiveBindTimeouts += 1
            AppLog.Shizuku.w(
                "bind timeout (${BIND_TIMEOUT_MS / 1000}s): privileged subprocess never started. " +
                    "Search Logcat (no filter): ShizukuServiceStarter, user_service_created. " +
                    "attempt=$consecutiveBindTimeouts/${SERVICE_BLOCKED_AFTER_TIMEOUTS}"
            )
            logStatus("bind_timeout")
            if (consecutiveBindTimeouts < SERVICE_BLOCKED_AFTER_TIMEOUTS && hasShizukuAccess()) {
                mainHandler.postDelayed({
                    AppLog.Shizuku.i(
                        "bind_auto_retry ${consecutiveBindTimeouts + 1}/$SERVICE_BLOCKED_AFTER_TIMEOUTS"
                    )
                    bindUserServiceIfNeeded(lastContext)
                }, 1_500L)
            } else if (isUserServiceBlocked()) {
                AppLog.Shizuku.e(
                    "Shizuku UserService blocked by ROM/OEM after $consecutiveBindTimeouts bind timeouts " +
                        "(no user_service_connected / no user_service_created). " +
                        "Shizuku binder+permission may be OK; app_process subprocess failed to start. " +
                        "Use Speaker boost + AMR fallback."
                )
            }
        }
        bindTimeoutRunnable = runnable
        mainHandler.postDelayed(runnable, BIND_TIMEOUT_MS)
    }

    private fun cancelBindTimeout() {
        bindTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        bindTimeoutRunnable = null
    }

    private fun userServiceComponent(context: Context): ComponentName =
        ComponentName(context.packageName, USER_SERVICE_CLASS)

    /** Matches official Shizuku demo: daemon=false, short suffix, no custom tag, debuggable=false. */
    private fun userServiceArgs(component: ComponentName): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(component)
            .daemon(false)
            .debuggable(false)
            .version(USER_SERVICE_VERSION)
            .processNameSuffix(PROCESS_NAME_SUFFIX)

    private fun tryAttachExistingUserService(component: ComponentName): Boolean {
        val args = userServiceArgs(component)
        return runCatching {
            val code = Shizuku.peekUserService(args, serviceConnection)
            AppLog.Shizuku.detail("peek_user_service", "code" to code)
            if (code != -1) {
                bindRequested = false
                consecutiveBindTimeouts = 0
                logStatus("peek_reattached")
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun unbindUserService() {
        val ctx = lastContext ?: return
        if (!bindRequested && recordingService == null) return
        cancelBindTimeout()
        runCatching {
            Shizuku.unbindUserService(userServiceArgs(userServiceComponent(ctx)), serviceConnection, true)
        }
        bindRequested = false
        recordingService = null
        logStatus("unbound")
    }

    fun getService(): IShizukuRecordingService? = recordingService

    fun resetBindStateForRetest() {
        cancelBindTimeout()
        consecutiveBindTimeouts = 0
        bindRequested = false
        bindCycleStarted = false
        AppLog.Shizuku.i("Shizuku bind cycle reset")
        logStatus("bind_state_reset")
    }

    fun launchShizukuFlow(context: Context) {
        lastContext = context.applicationContext
        resetBindStateForRetest()
        if (!isShizukuInstalled(context)) {
            openShizukuInstallPage(context)
            return
        }
        openShizukuManager(context)
    }

    fun resumeSetup(context: Context) {
        resumeSetupInternal(context, wakeBinderIfMissing = false)
    }

    fun resumeSetupAfterShizukuToggle(context: Context) {
        resetBindStateForRetest()
        resumeSetupInternal(context, wakeBinderIfMissing = true)
    }

    fun wakeBinderAfterShizukuEnable(context: Context) {
        val ctx = context.applicationContext
        val intent = android.content.Intent(ctx, ShizukuBinderRefreshActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    private fun resumeSetupInternal(context: Context, wakeBinderIfMissing: Boolean) {
        lastContext = context.applicationContext
        logStatus("resume_setup")
        if (!isShizukuInstalled(context)) return
        if (!isBinderAvailable()) {
            if (wakeBinderIfMissing) wakeBinderAfterShizukuEnable(context)
            return
        }
        if (!hasPermission()) {
            requestPermission()
            return
        }
        if (isReady()) return
        if (isUserServiceBlocked()) return
        if (isBindingInProgress()) return
        if (bindCycleStarted) return
        mainHandler.postDelayed({
            bindUserServiceIfNeeded(context)
        }, if (wakeBinderIfMissing) 3_000L else 2_000L)
    }

    fun openShizukuManager(context: Context) {
        for (pkg in managerPackages) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                context.startActivity(launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
        }
        openShizukuInstallPage(context)
    }

    fun openSetupGuide(context: Context) {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(SETUP_GUIDE_URL)
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openShizukuInstallPage(context: Context) {
        val market = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$PLAY_STORE_PACKAGE")
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        if (market.resolveActivity(context.packageManager) != null) {
            context.startActivity(market)
            return
        }
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE")
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun shizukuUid(): Int =
        if (isBinderAvailable()) runCatching { Shizuku.getUid() }.getOrElse { -1 } else -1

    fun logStatus(reason: String) {
        AppLog.Shizuku.detail(
            reason,
            "installed" to (lastContext?.let { isShizukuInstalled(it) } ?: false),
            "binderPing" to isBinderAvailable(),
            "permission" to hasPermission(),
            "shizukuAccess" to hasShizukuAccess(),
            "bindRequested" to bindRequested,
            "serviceConnected" to (recordingService != null),
            "ready" to isReady(),
            "readinessReason" to readinessReason(),
            "bindTimeouts" to consecutiveBindTimeouts,
            "shizukuUid" to shizukuUid()
        )
    }
}
