package org.mozilla.scryer.telemetry

import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.Nullable
import org.mozilla.scryer.BuildConfig
import org.mozilla.scryer.R
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage


class TelemetryWrapper {

    private object Category {
        val ACTION = "action"
    }

    private object Method {
        val FOREGROUND = "foreground"
        val BACKGROUND = "background"
    }

    private object Object {
        val APP = "app"
    }

    companion object {

        private const val TELEMETRY_APP_NAME = "Scryer"

        fun init(context: Context) {
            try {
                val resources = context.resources
                val telemetryEnabled = isTelemetryEnabled(context)

                val configuration = TelemetryConfiguration(context)
                        .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                        .setAppName(TELEMETRY_APP_NAME)
                        .setUpdateChannel(BuildConfig.BUILD_TYPE)
                        .setPreferencesImportantForTelemetry(
                                resources.getString(R.string.pref_key_enable_capture_service),
                                resources.getString(R.string.pref_key_enable_floating_screenshot_button),
                                resources.getString(R.string.pref_key_enable_add_to_collection))
                        .setCollectionEnabled(telemetryEnabled)
                        .setUploadEnabled(telemetryEnabled)

                val serializer = JSONPingSerializer()
                val storage = FileTelemetryStorage(configuration, serializer)
                val client = HttpURLConnectionTelemetryClient()
                val scheduler = JobSchedulerTelemetryScheduler()

                TelemetryHolder.set(Telemetry(configuration, storage, client, scheduler)
                        .addPingBuilder(TelemetryCorePingBuilder(configuration))
                        .addPingBuilder(TelemetryEventPingBuilder(configuration)))
            } finally {
            }
        }

        fun startSession() {
            TelemetryHolder.get().recordSessionStart()
            EventBuilder(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
        }

        fun stopSession() {
            TelemetryHolder.get().recordSessionEnd()
            EventBuilder(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
        }

        fun stopMainActivity() {
            TelemetryHolder.get()
                    .queuePing(TelemetryCorePingBuilder.TYPE)
                    .queuePing(TelemetryEventPingBuilder.TYPE)
                    .scheduleUpload()
        }

        private fun isTelemetryEnabled(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isEnabledByDefault = BuildConfig.BUILD_TYPE == "release"
            return preferences.getBoolean(context.resources.getString(R.string.pref_key_enable_send_usage_data), isEnabledByDefault)
        }
    }

    internal class EventBuilder @JvmOverloads constructor(category: String, method: String, @Nullable `object`: String, value: String? = null) {
        var telemetryEvent: TelemetryEvent = TelemetryEvent.create(category, method, `object`, value)
        //TODO: Add firebase event

        fun extra(key: String, value: String): EventBuilder {
            telemetryEvent.extra(key, value)
            return this
        }

        fun queue() {
            val context = TelemetryHolder.get().configuration.context
            if (context != null) {
                telemetryEvent.queue()
            }
        }
    }
}