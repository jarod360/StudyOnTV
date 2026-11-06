package com.studyontv.tv

import org.json.JSONObject

/** Persisted time-limited parent bypass. */
object ParentMode {
    private const val KEY = "parent_mode_until"
    fun until(store: LocalStore): Long = store.get(KEY, "0").toLongOrNull() ?: 0L
    fun active(store: LocalStore): Boolean = until(store) > System.currentTimeMillis()
    @Synchronized fun set(store: LocalStore, minutes: Int) {
        require(minutes in listOf(0, 60, 120, 240)) { "只允许关闭或 1、2、4 小时" }
        val expiry = if (minutes == 0) 0L else System.currentTimeMillis() + minutes * 60000L
        store.set(KEY, expiry.toString())
    }
    fun status(store: LocalStore): JSONObject {
        val expiry = until(store)
        return JSONObject().put("active", expiry > System.currentTimeMillis())
            .put("until", expiry)
            .put("remainingSeconds", (expiry - System.currentTimeMillis()).coerceAtLeast(0L) / 1000)
    }
}
