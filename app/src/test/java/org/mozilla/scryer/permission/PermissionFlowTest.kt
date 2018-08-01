package org.mozilla.scryer.permission

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.ArgumentMatchers.*
import org.mozilla.scryer.MainActivity
import kotlin.reflect.KClass

class PermissionFlowTest {

    private lateinit var permissionState: PermissionFlow.PermissionStateProvider
    private lateinit var pageState: PermissionFlow.PageStateProvider

    @Mock
    private lateinit var viewDelegate: PermissionFlow.ViewDelegate

    private var permissions = mutableListOf(false, false)
    private var shouldShowStorageRational = false

    @Captor
    private lateinit var runnableCaptor: ArgumentCaptor<Runnable>


    @Before
    fun setUp() {
        permissions = mutableListOf(false, false, false)
        shouldShowStorageRational = false
        permissionState = object : PermissionFlow.PermissionStateProvider {

            override fun isStorageGranted(): Boolean {
                return permissions[0]
            }

            override fun isOverlayGranted(): Boolean {
                return permissions[1]
            }

            override fun shouldShowStorageRational(): Boolean {
                return shouldShowStorageRational
            }
        }

        pageState = object : PermissionFlow.PageStateProvider {
            private val results = mutableListOf(false, false, false)

            override fun isWelcomePageShown(): Boolean {
                return results[0]
            }

            override fun isOverlayPageShown(): Boolean {
                return results[1]
            }

            override fun isCapturePageShown(): Boolean {
                return results[2]
            }

            override fun setWelcomePageShown() {
                results[0] = true
            }

            override fun setOverlayPageShown() {
                results[1] = true
            }

            override fun setCapturePageShown() {
                results[2] = true
            }
        }

        MockitoAnnotations.initMocks(this)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun firstLaunch_showWelcomePage_clickToRequestStorage() {
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)
        flow.start()

        // Test: Show welcome page
        verifyMethod().showWelcomePage(capture(runnableCaptor))

        // Test: Click to request storage permission
        runnableCaptor.value.run()
        verifyMethod().requestStoragePermission()
    }

    @Test
    fun noOverlay_grantStorage_showOverlayPage() {
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)
        grantStorage(flow)
        verifyMethod().onStorageGranted()
        verifyMethod().showOverlayPermissionView(any(), any())
    }

    @Test
    fun hasOverlay_grantStorage_showCapturePage() {
        permissions[1] = true
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)
        grantStorage(flow)
        verifyMethod().onStorageGranted()
        verifyMethod().showCapturePermissionView(any(), any())
    }

    @Test
    fun denyStorage_showStoragePage_clickToRequestAgain() {
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)

        // Setup: Deny permission
        denyStorage(flow, false)

        // Test: Show storage page
        verifyMethod().showStoragePermissionView(anyString(), capture(runnableCaptor))

        // Test: Click action button to request again
        runnableCaptor.value.run()
        verifyMethod().requestStoragePermission()
    }

    @Test
    fun denyStorageForever_showStoragePage_clickToLaunchSetting() {
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)

        // Setup: Deny permission
        denyStorage(flow, true)

        // Test: Show storage page
        verifyMethod().showStoragePermissionView(anyString(), capture(runnableCaptor))

        // Test: Click action button to launch setting
        runnableCaptor.value.run()
        verifyMethod().launchSystemSettingPage()
    }

    @Test
    fun overlayPage_clickToRequestOverlay() {
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)
        grantStorage(flow)

        verifyMethod().showOverlayPermissionView(capture(runnableCaptor), any())

        runnableCaptor.value.run()
        verifyMethod().requestOverlayPermission()
    }

    @Test
    fun overlayPage_denyOverlay_clickToRequestOverlay() {
        val flow = PermissionFlow(permissionState, pageState, viewDelegate)
        grantStorage(flow)

        verifyMethod().showOverlayPermissionView(capture(runnableCaptor), any())

        runnableCaptor.value.run()
        verifyMethod().requestOverlayPermission()
    }

    private fun verifyMethod(): PermissionFlow.ViewDelegate {
        return Mockito.verify<PermissionFlow.ViewDelegate>(this.viewDelegate)
    }

    private fun grantStorage(flow: PermissionFlow) {
        permissions[0] = true
        flow.onPermissionResult(MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION, booleanArrayOf(true))
        flow.start()
    }

    private fun denyStorage(flow: PermissionFlow, dontAskAgain: Boolean) {
        permissions[0] = false
        flow.onPermissionResult(MainActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION, booleanArrayOf(false))
        shouldShowStorageRational = !dontAskAgain
        flow.start()
    }

    private fun <T> any(): T {
        Mockito.any<T>()
        return castNull()
    }

    inline fun <reified T : Any> capture(captor: ArgumentCaptor<T>): T {
        return captor.capture() ?: createInstance()
    }

    inline fun <reified T : Any> createInstance(): T {
        return createInstance(T::class)
    }

    fun <T : Any> createInstance(kClass: KClass<T>): T {
        return castNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> castNull(): T = null as T
}