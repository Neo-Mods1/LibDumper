package com.neomods.libdumper.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar

class CrashActivity : Activity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private var fullLog: String = ""

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"
        const val EXTRA_CRASH_MESSAGE = "crash_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.setBackgroundColor(Color.parseColor("#1A1A2E"))
        window.statusBarColor = Color.parseColor("#1A1A2E")
        window.navigationBarColor = Color.parseColor("#1A1A2E")

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val titleText = TextView(this).apply {
            text = "Crash Report"
            setTextColor(Color.parseColor("#FF6B6B"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        }
        rootLayout.addView(titleText)

        val messageText = TextView(this).apply {
            text = intent.getStringExtra(EXTRA_CRASH_MESSAGE) ?: "Unknown error occurred"
            setTextColor(Color.parseColor("#A0A0B0"))
            textSize = 14f
            setPadding(0, 0, 0, dp(16))
            setLineSpacing(0f, 1.2f)
        }
        rootLayout.addView(messageText)

        val loadingText = TextView(this).apply {
            text = "Streaming logs..."
            setTextColor(Color.parseColor("#FFB347"))
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
            visibility = View.VISIBLE
        }
        rootLayout.addView(loadingText)

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            setPadding(0, 0, 0, dp(16))
            visibility = View.VISIBLE
        }
        rootLayout.addView(progressBar)

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#0F0F23"))
            isHorizontalScrollBarEnabled = true
        }

        logTextView = TextView(this).apply {
            setTextColor(Color.parseColor("#00FF41"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.3f)
            setTextIsSelectable(true)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        scrollView.addView(logTextView)
        rootLayout.addView(scrollView)

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, 0)
            gravity = Gravity.CENTER
        }

        val copyButton = Button(this).apply {
            text = "Copy Log"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4ECDC4"))
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setOnClickListener { copyLogToClipboard() }
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginEnd = dp(8) }
        }
        buttonLayout.addView(copyButton)

        val restartButton = Button(this).apply {
            text = "Restart App"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6C5CE7"))
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setOnClickListener { restartApp() }
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(8) }
        }
        buttonLayout.addView(restartButton)

        rootLayout.addView(buttonLayout)

        setContentView(rootLayout)

        fullLog = intent.getStringExtra(EXTRA_CRASH_LOG) ?: "No crash log available"
        
        loadingText.visibility = View.GONE
        progressBar.visibility = View.GONE
        logTextView.text = fullLog
    }

    private fun copyLogToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", fullLog)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        finish()
        Process.killProcess(Process.myPid())
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
