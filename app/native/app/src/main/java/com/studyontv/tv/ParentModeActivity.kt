package com.studyontv.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.view.inputmethod.InputMethodManager
import android.content.Context

/** Reuse the actual parent dashboard and its authenticated bypass controls. */
class ParentModeActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeviceBridge.isTv(this)) { finish(); return }
        DeviceRuntime.ensure(this)
        web = WebShell(this).create(Prefs.localBase(this) + "admin/?tvParentMode=1")
        web.addJavascriptInterface(ParentBridge(), "SOTVParent")
        setContentView(web)
        web.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> "move(-1)"
            KeyEvent.KEYCODE_DPAD_DOWN -> "move(1)"
            KeyEvent.KEYCODE_DPAD_LEFT -> "horizontal(-1)"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "horizontal(1)"
            KeyEvent.KEYCODE_DPAD_CENTER -> "select()"
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "submit()"
            else -> return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) web.evaluateJavascript("window.TV&&TV.$action", null)
        return true
    }

    private inner class ParentBridge {
        @JavascriptInterface fun showKeyboard() = runOnUiThread {
            if (!isFinishing) (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(web, InputMethodManager.SHOW_IMPLICIT)
        }
        @JavascriptInterface fun modeChanged() = runOnUiThread {
            if (isFinishing) return@runOnUiThread
            GateService.hideGates()
            if (ParentMode.active(DeviceRuntime.store)) {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else startActivity(Intent(this@ParentModeActivity, MainActivity::class.java))
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        // A new visit must authenticate again, like the old native password form.
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.removeJavascriptInterface("SOTVParent")
            web.destroy()
        }
        super.onDestroy()
    }
}
