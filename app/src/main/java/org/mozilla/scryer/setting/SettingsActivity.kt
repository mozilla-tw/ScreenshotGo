package org.mozilla.scryer.setting

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val repository = ScryerApplication.getSettingsRepository()

        val switch = findViewById<SwitchCompat>(R.id.floating_enabled_switch)
        switch.isChecked = repository.isFloatingEnabled()
        switch.setOnCheckedChangeListener { _, isChecked ->
            repository.setFloatingEnabled(isChecked)
        }
    }
}
