package com.studyontv.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView

open class MainActivity : Activity() {
    protected lateinit var web: WebView
    private var webReleased = false
    private var page = "quiz"
    private var upCount = 0
    private var lastUp = 0L
    private var downCount = 0
    private var lastDown = 0L
    private val parentHandler = Handler(Looper.getMainLooper())
    /** Parent explicitly launches the TV app during bypass: show authenticated controls. */
    private fun showParentModeIfActive(): Boolean {
        if (isFinishing || this is GateActivity || !DeviceBridge.isTv(this) ||
            !ParentMode.active(DeviceRuntime.store)) return false
        parentHandler.removeCallbacks(parentCheck)
        startActivity(Intent(this, ParentModeActivity::class.java))
        finish()
        return true
    }
    private val parentCheck = object : Runnable {
        override fun run() {
            if (isFinishing) return
            if (ParentMode.active(DeviceRuntime.store)) {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                if (this@MainActivity is GateActivity) finish() else moveTaskToBack(true)
            } else parentHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AdminAuth.configured(this)) {
            startActivity(Intent(this, ParentActivity::class.java)); finish(); return
        }
        if (!Prefs.onboarded(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java)); finish(); return
        }
        DeviceRuntime.ensure(this)
        if (showParentModeIfActive()) {
            GateService.start(this)
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (DeviceBridge.isTv(this)) requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val reason = intent.getStringExtra("reason")
        page = if (this is GateActivity && reason != null && reason != "timeup") "lock?reason=$reason" else "quiz"
        createWeb()
        GateService.start(this)
        if (this !is GateActivity) Updater.check(this) { info ->
            val newer = Updater.newerThanCurrent(this, info) ?: return@check
            runOnUiThread {
                val raw = DeviceBridge(this).updateInfo()
                if (newer.force) startActivity(Intent(this, UpdateActivity::class.java).putExtra("payload", raw))
                else if (::web.isInitialized && !webReleased) web.evaluateJavascript("window.SOTV_UPDATE&&SOTV_UPDATE($raw)", null)
            }
        }
    }

    private fun createWeb() {
        web = WebShell(this).create(Prefs.localBase(this) + "#/$page")
        web.addJavascriptInterface(DeviceBridge(this, false, web), "SOTVNative")
        setContentView(web)
        web.requestFocus()
        webReleased = false
    }

    override fun onStop() {
        super.onStop()
        if (DeviceBridge.isTv(this) && ::web.isInitialized && !webReleased) {
            (web.parent as? android.view.ViewGroup)?.removeView(web)
            web.destroy()
            webReleased = true
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (showParentModeIfActive()) return
        val reason = intent?.getStringExtra("reason")
        page = if (this is GateActivity && reason != null && reason != "timeup") "lock?reason=$reason" else "quiz"
        if (::web.isInitialized && !webReleased) {
            web.loadUrl(Prefs.localBase(this) + "#/$page")
            web.evaluateJavascript("window.SOTV_RESUME&&SOTV_RESUME()", null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (showParentModeIfActive()) return
        parentHandler.removeCallbacks(parentCheck)
        parentHandler.post(parentCheck)
        if (webReleased) createWeb()
        if (::web.isInitialized && !webReleased) web.evaluateJavascript("window.SOTV_RESUME&&SOTV_RESUME()", null)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                val now = android.os.SystemClock.elapsedRealtime()
                upCount = if (now - lastUp < 1600) upCount + 1 else 1
                lastUp = now
                if (upCount >= 5) {
                    upCount = 0
                    startActivity(Intent(this, ParentActivity::class.java)); return true
                }
            } else upCount = 0
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN && DeviceBridge.isTv(this)) {
                val now = android.os.SystemClock.elapsedRealtime()
                downCount = if (now - lastDown < 1600) downCount + 1 else 1
                lastDown = now
                if (downCount >= 5) {
                    downCount = 0
                    startActivity(Intent(this, ParentModeActivity::class.java))
                    finish()
                    return true
                }
            } else downCount = 0
        }
        if (!DeviceBridge.isTv(this) && event.keyCode == KeyEvent.KEYCODE_ENTER) return super.dispatchKeyEvent(event)
        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> "move('left')"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "move('right')"
            KeyEvent.KEYCODE_DPAD_UP -> "move('up')"
            KeyEvent.KEYCODE_DPAD_DOWN -> "move('down')"
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "select()"
            KeyEvent.KEYCODE_BACK -> "back()"
            else -> return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN && ::web.isInitialized && !webReleased) web.evaluateJavascript("window.TV&&TV.$action", null)
        return true
    }

    override fun onPause() {
        parentHandler.removeCallbacks(parentCheck)
        super.onPause()
    }

    override fun onDestroy() {
        parentHandler.removeCallbacks(parentCheck)
        if (::web.isInitialized && !webReleased) web.destroy()
        super.onDestroy()
    }
}
