package org.mozilla.scryer.telemetry

import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.Nullable
import org.mozilla.scryer.BuildConfig
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.SettingsMeasurement
import org.mozilla.telemetry.measurement.TelemetryMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage


class TelemetryWrapper {

    private object Category {
        const val ACTION = "action"
    }

    private object Method {
        const val FOREGROUND = "foreground"
        const val BACKGROUND = "background"
        const val CLICK = "click"
        const val SHOW = "show"
    }

    private object Object {
        const val APP = "app"
        const val WELCOME_PAGE = "welcome_page"
        const val WELCOME_STORAGE_PERMISSION = "welcome_storage_permission"
        const val WELCOME_OVERLAY_PERMISSION = "welcome_overlay_permission"
        const val HOME_PAGE = "home_page"
        const val HOME_SEARCH_BAR = "home_search_bar"
        const val HOME_QUICK_ACCESS = "home_quick_access"
        const val HOME_COLLECTIONS = "home_collections"
        const val HOME_CREATE_NEW_COLLECTION = "home_create_new_collection"
        const val HOME_SETTINGS = "home_settings"
        const val COLLECTION_PAGE = "collection_page"
        const val COLLECTION_SORTING_BUTTON = "collection_sorting_button"
        const val COLLECTION_ITEM = "collection_item"
        const val SORTING_PAGE = "sorting_page"
        const val SORTING_MOVE_TO_BUTTON = "sorting_move_to_button"
        const val SORTING_SORT_CANCEL = "sorting_sort_cancel"
        const val CAPTURE_BUTTON = "capture_button"
        const val CAPTURE_VIA_NOTIFICATION = "capture_via_notification"
        const val CAPTURE_VIA_EXTERNAL = "capture_via_external"
        const val DETAIL_PAGE = "detail_page"
        const val DETAIL_SHARE_BUTTON = "detail_share_button"
        const val TEXT_MODE_BUTTON = "text_mode_button"
        const val TEXT_MODE_RESULT = "text_mode_result"
        const val SEARCH_PAGE = "search_page"
        const val SEARCH_INTERESTED = "search_interested"
        const val SEARCH_NOT_INTERESTED = "search_not_interested"
    }

    object Value {
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
        const val SUCCESS = "success"
        const val WEIRD_SIZE = "weird_size"
        const val FAIL = "fail"
    }

    private object Extra {
        const val ON = "on"
    }

    private object ExtraValue {
        const val MORE = "more"
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
                        .setSettingsProvider(CustomSettingsProvider())
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

        private fun isTelemetryEnabled(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isEnabledByDefault = BuildConfig.BUILD_TYPE == "release"
            return preferences.getBoolean(context.resources.getString(R.string.pref_key_enable_send_usage_data), isEnabledByDefault)
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

        fun showWelcomePage() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.WELCOME_PAGE).queue()
        }

        fun clickWelcomeStoragePermission() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.WELCOME_STORAGE_PERMISSION).queue()
        }

        fun showWelcomeOverlayPermission() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.WELCOME_OVERLAY_PERMISSION).queue()
        }

        fun clickWelcomeOverlayPermission(value: String) {
            EventBuilder(Category.ACTION, Method.CLICK, Object.WELCOME_OVERLAY_PERMISSION, value).queue()
        }

        fun showHomePage() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.HOME_PAGE).queue()
        }

        fun clickHomeSearchBar() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.HOME_SEARCH_BAR).queue()
        }

        fun clickHomeQuickAccessItem(index: Int) {
            EventBuilder(Category.ACTION, Method.CLICK, Object.HOME_QUICK_ACCESS).extra(Extra.ON, index.toString()).queue()
        }

        fun clickHomeQuickAccessMoreItem() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.HOME_QUICK_ACCESS).extra(Extra.ON, ExtraValue.MORE).queue()
        }

        fun clickHomeCollectionItem(index: Int) {
            EventBuilder(Category.ACTION, Method.CLICK, Object.HOME_COLLECTIONS).extra(Extra.ON, index.toString()).queue()
        }

        fun clickHomeCreateNewCollectionItem() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.HOME_CREATE_NEW_COLLECTION).queue()
        }

        fun clickHomeSettings() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.HOME_SETTINGS).queue()
        }

        fun showCollectionPage() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.COLLECTION_PAGE).queue()
        }

        fun clickCollectionItem() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.COLLECTION_ITEM).queue()
        }

        fun clickSortingInCollectionPage() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.COLLECTION_SORTING_BUTTON).queue()
        }

        fun showSortingPage() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.SORTING_PAGE).queue()
        }

        fun clickMoveToInSortingPage() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.SORTING_MOVE_TO_BUTTON).queue()
        }

        fun clickCancelInSortingPage() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.SORTING_SORT_CANCEL).queue()
        }

        fun clickCaptureButton() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.CAPTURE_BUTTON).queue()
        }

        fun clickCaptureViaNotification() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.CAPTURE_VIA_NOTIFICATION).queue()
        }

        fun clickCaptureViaExternal() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.CAPTURE_VIA_EXTERNAL).queue()
        }

        fun showDetailPage() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.DETAIL_PAGE).queue()
        }

        fun clickTextModeButton() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.TEXT_MODE_BUTTON).queue()
        }

        fun clickShareButtonInDetailPage() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.DETAIL_SHARE_BUTTON).queue()
        }

        fun showTextModeResult(value: String) {
            EventBuilder(Category.ACTION, Method.SHOW, Object.TEXT_MODE_RESULT, value).queue()
        }

        fun showSearchPage() {
            EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_PAGE).queue()
        }

        fun clickSearchInterested() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.SEARCH_INTERESTED).queue()
        }

        fun clickSearchNotInterested() {
            EventBuilder(Category.ACTION, Method.CLICK, Object.SEARCH_NOT_INTERESTED).queue()
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

    private class CustomSettingsProvider : SettingsMeasurement.SharedPreferenceSettingsProvider() {

        private val custom = HashMap<String, Any>(1)

        override fun update(configuration: TelemetryConfiguration) {
            super.update(configuration)
            
            addCustomPing(configuration, ScreenshotCountMeasurement())
        }


        internal fun addCustomPing(configuration: TelemetryConfiguration, measurement: TelemetryMeasurement) {
            var preferenceKeys: MutableSet<String>? = configuration.preferencesImportantForTelemetry
            if (preferenceKeys == null) {
                configuration.setPreferencesImportantForTelemetry()
                preferenceKeys = configuration.preferencesImportantForTelemetry
            }
            preferenceKeys!!.add(measurement.fieldName)
            custom[measurement.fieldName] = measurement.flush()
        }

        override fun containsKey(key: String): Boolean {
            return super.containsKey(key) or custom.containsKey(key)
        }

        override fun getValue(key: String): Any {
            return custom[key] ?: super.getValue(key)
        }
    }

    private class ScreenshotCountMeasurement : TelemetryMeasurement(MEASUREMENT_SCREENSHOT_COUNT) {

        override fun flush(): Any {
            if ("main" == Thread.currentThread().name) {
                throw RuntimeException("Call from main thread exception")
            }

            return try {
                ScryerApplication.getScreenshotRepository().getScreenshotList().size
            } catch (e: Exception) {
                -1
            }
        }

        companion object {
            private const val MEASUREMENT_SCREENSHOT_COUNT = "screenshot_count"
        }
    }
}