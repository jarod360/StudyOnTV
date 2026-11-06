package com.studyontv.tv

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

object Foreground {
    @Volatile var lastPackage: String? = null
        private set
    private var ownActivity: java.lang.ref.WeakReference<android.app.Activity>? = null

    fun resumeOwn(activity: android.app.Activity) {
        ownActivity = java.lang.ref.WeakReference(activity)
        update(activity.packageName)
    }
    fun pauseOwn(activity: android.app.Activity) {
        if (ownActivity?.get() === activity) ownActivity = null
        if (ownActivity?.get() == null && lastPackage == activity.packageName) lastPackage = null
    }
    fun update(pkg: String?) { if (!pkg.isNullOrEmpty()) lastPackage = pkg }
    fun clearExternal() { lastPackage = null }

    private val launcherPackages = setOf(
        "com.android.tv.launcher", "com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx",
        "com.android.launcher", "com.android.launcher3", "com.tcl.launcher", "com.mitv.tvhome",
        "com.hisense.tv.launcher", "com.google.android.apps.nexuslauncher", "com.miui.home",
        "com.sec.android.app.launcher", "com.huawei.android.launcher", "com.oppo.launcher", "com.vivo.launcher"
    )
    fun isLauncher(pkg: String?): Boolean = pkg != null && launcherPackages.contains(pkg)

    @Suppress("DEPRECATION")
    fun current(context: Context): String? {
        ownActivity?.get()?.let {
            if (it.hasWindowFocus() && (Build.VERSION.SDK_INT < 24 || !it.isInMultiWindowMode)) return it.packageName
        }
        val fallback = if (WatchAccessibilityService.connected) lastPackage else null
        // Sony AirPlay may change the resumed Activity without sending an accessibility
        // window event. Prefer the system usage record over a stale accessibility package.
        if (!hasUsageAccess(context)) return fallback
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = manager.queryEvents(now - 86400000, now)
            val event = UsageEvents.Event()
            var latest: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) latest = event.packageName
            }
            latest ?: manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 86400000, now)
                ?.filter { it.lastTimeUsed > 0 }?.maxByOrNull { it.lastTimeUsed }?.packageName ?: fallback
        } catch (_: Exception) { fallback }
    }

    fun hasUsageAccess(context: Context): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
}
