package com.studyontv.tv

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import org.json.JSONObject

/** On-demand update entry shared by the two TV parent settings screens. */
object TvUpdateEntry {
    fun check(activity: Activity) {
        Toast.makeText(activity, "正在检查 GitHub 新版本…", Toast.LENGTH_SHORT).show()
        Updater.check(activity, forceRefresh = true) { info ->
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                if (info == null) {
                    Toast.makeText(activity, "检查失败，请检查电视网络和 GitHub 连接", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val newer = Updater.newerThanCurrent(activity, info)
                if (newer == null) {
                    Toast.makeText(activity, "当前已是最新版本：" + Updater.currentVersion(activity), Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val payload = JSONObject()
                    .put("latest", newer.latest).put("current", newer.current)
                    .put("url", newer.url).put("notes", newer.notes)
                    .put("force", newer.force).toString()
                activity.startActivity(Intent(activity, UpdateActivity::class.java).putExtra("payload", payload))
            }
        }
    }
}
