package com.studyontv.tv

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class StudyDeviceAdminReceiver : DeviceAdminReceiver()

object DeviceAdminState {
    fun active(context: Context): Boolean = try {
        val policy = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        policy.isAdminActive(ComponentName(context, StudyDeviceAdminReceiver::class.java))
    } catch (_: Exception) { false }
}
