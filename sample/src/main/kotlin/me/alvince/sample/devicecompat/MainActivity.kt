package me.alvince.sample.devicecompat

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import me.alvince.android.devicecompat.DeviceHelper
import me.alvince.android.devicecompat.DisplayHelper

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            DisplayHelper.makeContentImmersive(this)
            appbar.setPadding(0, DisplayHelper.getStatusBarSize(this), 0, 0)
        }

        text_overview.text = "---------------------------"
                .plus("\nBRAND     : ${Build.BRAND}")
                .plus("\nDEVICE    : ${Build.DEVICE}")
                .plus("\nDISPLAY   : ${Build.DISPLAY}")
                .plus("\nPRODUCT   : ${Build.PRODUCT}")
                .plus("\nPad       : ${if (DeviceHelper.isPad()) "✓" else "✗"}")
                .plus("\nNavigation: ${if (DeviceHelper.hasNavigationBar(this)) "✓" else "✗"}")
                .plus("\nNotch     : ${if (DeviceHelper.hasNotchInScreen(this)) "✓" else "✗"}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            btn_status_bart_mode.visibility = View.VISIBLE
            btn_status_bart_mode.setOnClickListener { toggleSystemUiMode() }
        } else {
            btn_status_bart_mode.visibility = View.GONE
        }
        btn_hide_system_ui_fullscreen.setOnClickListener { toggleSystemUi(true) }
        btn_hide_system_ui_navigation.setOnClickListener { toggleSystemUi(false) }
    }

    private fun toggleSystemUi(fullscreen: Boolean) {
        val contentView =
                window?.decorView?.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
        contentView ?: return

        DisplayHelper.hideSystemUi(contentView, fullscreen)
    }

    private fun toggleSystemUiMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val light = DeviceHelper.makeSystemUiReverse(this, !btn_status_bart_mode.isSelected)
            (btn_status_bart_mode as TextView).text = getString(
                    if (light) R.string.button_status_bar_dark else R.string.button_status_bar_light)
            btn_status_bart_mode.isSelected = light

            appbar.setBackgroundColor(
                    if (light) Color.TRANSPARENT else ContextCompat.getColor(this, R.color.colorPrimary))
            toolbar.setTitleTextColor(if (light) Color.BLACK else Color.WHITE)
        }
    }
}
