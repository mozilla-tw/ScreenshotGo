# Telemetry
For clients that have "send usage data" enabled, ScreenshotGo sends a "core" ping and an "event" ping to Mozilla's telemetry service. Sending telemetry can be disabled in the app's settings. Builds of "ScreenshotGo" have telemetry enabled by default ("opt-out").

Note: ScreenshotGo is still in development and this documentation is subject to change.

# Install campaign tracking

We use [Adjust SDK](https://github.com/adjust/android_sdk) to evaluate the effectiveness of our campaign channel. What data has been collected can be found in the code [here](https://github.com/adjust/android_sdk/blob/0abfac3ace0c10795d78f4d89e2c607d41d13426/Adjust/adjust/src/main/java/com/adjust/sdk/PackageBuilder.java#L210). The evaluation process is accomplished by 1. Receive [INSTALL_REFERRER Intent](https://developer.android.com/reference/com/google/android/gms/tagmanager/InstallReferrerReceiver.html) from Google Play; 2. Send the referrer string back to [adjust server(https://app.adjust.com)](https://github.com/adjust/android_sdk/blob/0abfac3ace0c10795d78f4d89e2c607d41d13426/Adjust/adjust/src/main/java/com/adjust/sdk/Constants.java#L29) when the application is [alive](https://developer.android.com/reference/android/app/Application.html#onCreate%28%29).

# ScreenshotGo uses Firebase to improve our product

ScreenshotGo also uses Google’s [Firebase](https://firebase.google.com/) platform to help us improve performance, support crashes, understand your experience with ScreenshotGo and improve the user experience through A/B testing and in-product messaging. Below are the specific Firebase Products we use
* Google Analytics; to analyze anonymous user attributions and behavior to make informed decisions on our product roadmap. 
* Remote Config; to customize your ScreenshotGo experience such as changing the look and feel, rolling out features gradually, running A/B tests, delivering customized content, or making other updates without deploying a new version.
* Cloud Messaging; to send messages and notifications so we can bring contextual hints to you to help you use ScreenshotGo better 
* Crashlytics; to understand crashes better so you can use ScreenshotGo without problems  
* Performance Monitoring; to diagnose app performance issues so we can keep ScreenshotGo really fast

Learn more about Firebase data collection [here](https://support.google.com/firebase/answer/6318039) or you have the option to turn off Firebase by disabling “Send Usage Data” in your Settings. For events automatically collected by Firebase, see [here](https://support.google.com/firebase/answer/6317485?hl=en)

# Core ping

ScreenshotGo creates and tries to send a "core" ping whenever the app goes to the background. This core ping uses the same format as Firefox for Android and is [documented on firefox-source-docs.mozilla.org](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/data/core-ping.html).


# Event ping

In addition to the core ping an event ping for UI telemetry is generated and sent as soon as the app is sent to the background. All the telemetry mechanism is leverage from the library [telemetry-android](https://github.com/mozilla-mobile/telemetry-android) which is also been used in [Firefox Focus for Android](https://github.com/mozilla-mobile/focus-android).

## Settings

As part of the event ping the most recent state of the user's setting is sent (default values in **bold**):

| Setting                           | Key                                        | Value
|-----------------------------------|--------------------------------------------|----------------------
| Enable Capture Service            | pref_key_enable_capture_service            | **true** / false
| Enable Floating Screenshot Button | pref_key_enable_floating_screenshot_button | **true** / false
| Enable Add to Collection          | pref_key_enable_add_to_collection          | **true** / false
 
## Events

The event ping contains a list of events ([see event format on firefox-source-docs.mozilla.org](https://firefox-source-docs.mozilla.org/toolkit/components/telemetry/telemetry/collection/events.html)) for the following actions:

### Sessions

| Event                                    | category | method     | object | value  |
|------------------------------------------|----------|------------|--------|--------|
| Start session (App is in the foreground) | action   | foreground | app    |        |
| Stop session (App is in the background)  | action   | background | app    |        |

### Welcome

| Event             | category | method  |   object                   |    value    |   extra    |
|-------------------|----------|---------|----------------------------|-------------|------------|
| Welcome           | action   | show    | welcome_page               |             |            |
| Let's go          | action   | click   | welcome_storage_permission |             |            |
| Overlay permission| action   | show    | welcome_overlay_permission |             |            |
| Overlay permission| action   | click   | welcome_overlay_permission | positive    |            |
| Overlay permission| action   | click   | welcome_overlay_permission | negative    |            |

### Home

| Event             | category | method  |   object                   |    value    |   extra    |
|-------------------|----------|---------|----------------------------|-------------|------------|
| Search            | action   | click   | home_search_bar            |             |            |
| Quick access      | action   | click   | home_quick_access          |             | on:[index] |
| Quick access      | action   | click   | home_quick_access          |             | on: more   |
| Collections       | action   | click   | home_collections           |             |            |
| Create collection | action   | click   | home_create_new_collection |             |            |
| Settings          | action   | click   | home_settings              |             |            |

### Capture

| Event                    | category | method  |   object                   |    value    |   extra    |
|--------------------------|----------|---------|----------------------------|-------------|------------|
| Capture                  | action   | click   | capture_button             |             |            |
| Capture via notification | action   | click   | capture_via_notification   |             |            |
| Capture via external     | action   | show    | capture_via_external       |             |            |

### Detail

| Event             | category | method  |   object                   |    value    |   extra    |
|-------------------|----------|---------|----------------------------|-------------|------------|
| Text mode         | action   | click   | text_mode_button           |             |            |

### Search

| Event             | category | method  |   object                   |    value    |   extra    |
|-------------------|----------|---------|----------------------------|-------------|------------|
| Interested        | action   | click   | search_interested          |             |            |
| Not interested    | action   | click   | search_not_interested      |             |            |

## Limits

* An event ping will contain up to but no more than 500 events
* No more than 40 pings per type (core/event) are stored on disk for upload at a later time
* No more than 100 pings are sent per day

# Implementation notes

* Event pings are generated (and stored on disk) whenever the onStop() callback of the main activity is triggered. This happens whenever the main screen of the app is no longer visible (The app is in the background or another screen is displayed on top of the app).

* Whenever we are storing pings we are also scheduling an upload. We are using Android’s JobScheduler API for that. This allows the system to run the background task whenever it is convenient and certain criteria are met. The only criteria we are specifying is that we require an active network connection. In most cases, this job is executed immediately after the app is in the background.

* Whenever an upload fails we are scheduling a retry. The first retry will happen after 30 seconds (or later if there’s no active network connection at this time). For further retries a exponential backoff policy is used: [30 seconds] * 2 ^ (num_failures - 1)

* An earlier retry of the upload can happen whenever the app is coming to the foreground and sent to the background again (the previously scheduled job is reset and we are starting all over again).
