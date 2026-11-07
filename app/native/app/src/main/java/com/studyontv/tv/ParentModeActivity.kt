package com.studyontv.tv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Native TV parent shortcut, using the same form and controls as ParentActivity. */
class ParentModeActivity : Activity() {
    private lateinit var content: LinearLayout
    private var verified = false
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float, padded: Boolean = true) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(0xFF303B31.toInt())
        if (padded) setPadding(0, dp(10), 0, dp(10))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeviceBridge.isTv(this)) { finish(); return }
        DeviceRuntime.ensure(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
            setBackgroundColor(0xFFF8F9F6.toInt())
        }
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(content) })
        showPassword()
    }

    private fun showPassword() {
        verified = false
        content.removeAllViews()
        content.addView(label("家长验证", 28f))
        content.addView(label("请输入管理密码", 16f))
        val password = EditText(this).apply {
            id = View.generateViewId()
            hint = "管理密码"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            learningField()
            tvPasswordMask()
        }
        content.addView(password, LinearLayout.LayoutParams(-1, dp(51)))
        val error = label("", 14f).apply { setTextColor(0xFFFFB020.toInt()) }
        content.addView(error)
        val verify = Button(this).apply {
            id = View.generateViewId()
            text = "验证并进入免答题设置"
            parentActionStyle(primary = true)
        }
        verify.setOnClickListener {
            val candidate = password.text.toString()
            if (candidate.isEmpty()) {
                error.text = "请输入管理密码"
                return@setOnClickListener
            }
            verify.isEnabled = false
            password.isEnabled = false
            Thread {
                val valid = AdminAuth.verify(this, candidate)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (valid) { verified = true; showOptions() }
                    else {
                        verify.isEnabled = true
                        password.isEnabled = true
                        error.text = "密码错误，连续错误 5 次后需等待一分钟"
                        password.requestFocus()
                    }
                }
            }.start()
        }
        password.nextFocusUpId = password.id
        password.nextFocusDownId = verify.id
        verify.nextFocusUpId = password.id
        verify.nextFocusDownId = verify.id
        content.addView(verify, LinearLayout.LayoutParams(-1, dp(54)))
        password.requestFocus()
    }

    private fun showOptions() {
        content.removeAllViews()
        content.addView(label("家长免答题", 28f))
        content.addView(label("开启后暂停答题、不扣孩子观看余额，到期自动恢复。", 16f))
        val state = ParentMode.status(DeviceRuntime.store)
        val status = if (state.optBoolean("active"))
            "当前：已开启，剩余约 ${(state.optLong("remainingSeconds") + 59) / 60} 分钟"
            else "当前：正常答题管控"
        content.addView(label(status, 15f, false).apply { setTextColor(0xFF65735D.toInt()) })
        val actions = mutableListOf<Button>()
        fun action(title: String, onClick: () -> Unit) {
            val button = Button(this).apply {
                id = View.generateViewId()
                text = title
                parentActionStyle(primary = false)
                setOnClickListener { if (verified) onClick() }
            }
            content.addView(button, LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(if (actions.isEmpty()) 16 else 10)
            })
            actions.add(button)
        }
        listOf(60 to "免答题 1 小时", 120 to "免答题 2 小时", 240 to "免答题 4 小时",
            0 to "立即恢复管控").forEach { (minutes, title) ->
            action(title) {
                ParentMode.set(DeviceRuntime.store, minutes)
                GateService.hideGates()
                if (minutes == 0) startActivity(Intent(this, MainActivity::class.java))
                else startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }
        action("返回答题") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        actions.forEachIndexed { index, button ->
            button.nextFocusUpId = actions[(index - 1).coerceAtLeast(0)].id
            button.nextFocusDownId = actions[(index + 1).coerceAtMost(actions.lastIndex)].id
        }
        actions.first().requestFocus()
    }

    override fun onStop() {
        super.onStop()
        if (verified && !isFinishing) showPassword()
    }
}
