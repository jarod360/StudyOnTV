package com.studyontv.tv

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/** AppOps MODE_DEFAULT is not an explicit background restriction. */
object BackgroundStatus {
    fun isAllowed(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT < 26) true else {
            val manager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val operations = if (Build.VERSION.SDK_INT >= 28)
                listOf(AppOpsManager.OPSTR_RUN_IN_BACKGROUND, AppOpsManager.OPSTR_RUN_ANY_IN_BACKGROUND)
            else listOf(AppOpsManager.OPSTR_RUN_IN_BACKGROUND)
            operations.all { op ->
                @Suppress("DEPRECATION")
                val mode = manager.checkOpNoThrow(op, Process.myUid(), context.packageName)
                mode != AppOpsManager.MODE_IGNORED && mode != AppOpsManager.MODE_ERRORED
            }
        }
    } catch (_: Exception) { true }
}
