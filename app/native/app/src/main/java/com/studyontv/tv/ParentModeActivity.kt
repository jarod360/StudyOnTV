package com.studyontv.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** TV-only shortcut opened after five short DPAD_DOWN presses. */
class ParentModeActivity : Activity() {
    private val bg = 0xFF0D0F13.toInt()
    private val panel = 0xFF171B21.toInt()
    private val white = 0xFFF3F4F6.toInt()
    private val muted = 0xFF9DA5AF.toInt()
    private val gold = 0xFFF0AD64.toInt()
    private lateinit var content: LinearLayout

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun shape(fill: Int, border: Int = fill): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(2), border)
        }
    private fun label(value: String, size: Float, color: Int = white): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_HORIZONTAL
        }
    private fun button(value: String, action: () -> Unit): Button =
        Button(this).apply {
            text = value
            textSize = 18f
            isAllCaps = false
            setTextColor(white)
            backgroundTintList = null
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), shape(panel, gold))
                addState(intArrayOf(), shape(panel, 0xFF353C46.toInt()))
            }
            setOnClickListener { action() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeviceBridge.isTv(this)) { finish(); return }
        DeviceRuntime.ensure(this)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(30), dp(26), dp(30), dp(26))
            background = shape(panel)
        }
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(bg)
            addView(content, LinearLayout.LayoutParams(dp(700), -2))
        }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(outer)
        })
        showPassword()
    }

    private fun showPassword() {
        content.removeAllViews()
        content.addView(label("家长免答题", 28f, gold))
        content.addView(label("输入家长管理密码后设置时长", 15f, muted),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        val password = EditText(this).apply {
            hint = "管理密码"
            textSize = 18f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(white)
            setHintTextColor(muted)
        }
        content.addView(password, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(20) })
        val error = label("", 14f, 0xFFFFB020.toInt())
        content.addView(error)
        val verify = button("验证并继续") {
            val candidate = password.text.toString()
            if (candidate.isEmpty()) {
                error.text = "请输入管理密码"
            } else {
                password.isEnabled = false
                Thread {
                    val ok = AdminAuth.verify(this, candidate)
                    runOnUiThread {
                        password.isEnabled = true
                        if (ok) showOptions() else error.text = "密码错误，连续错误 5 次将锁定一分钟"
                    }
                }.start()
            }
        }
        content.addView(verify, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) })
        content.addView(button("取消") { finish() },
            LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })
        password.requestFocus()
    }

    private fun showOptions() {
        content.removeAllViews()
        content.addView(label("家长免答题", 28f, gold))
        val state = ParentMode.status(DeviceRuntime.store)
        val info = if (state.optBoolean("active")) {
            "已开启，剩余约 ${(state.optLong("remainingSeconds") + 59) / 60} 分钟"
        } else "当前正常答题管控"
        content.addView(label(info, 16f, muted),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(16) })
        listOf(60 to "免答题 1 小时", 120 to "免答题 2 小时", 240 to "免答题 4 小时",
            0 to "立即恢复管控").forEach { (minutes, title) ->
            content.addView(button(title) {
                ParentMode.set(DeviceRuntime.store, minutes)
                GateService.hideGates()
                if (minutes == 0) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                finish()
            }, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(10) })
        }
        content.addView(button("返回") { finish() }, LinearLayout.LayoutParams(-1, dp(52)))
        content.getChildAt(2)?.requestFocus()
    }
}
