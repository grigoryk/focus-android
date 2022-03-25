/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.activity

import androidx.test.core.app.launchActivity
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import mozilla.components.browser.state.selector.privateTabs
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.activity.robots.customTab
import org.mozilla.focus.activity.robots.searchScreen
import org.mozilla.focus.ext.components
import org.mozilla.focus.helpers.FeatureSettingsHelper
import org.mozilla.focus.helpers.MainActivityFirstrunTestRule
import org.mozilla.focus.helpers.MockWebServerHelper
import org.mozilla.focus.helpers.RetryTestRule
import org.mozilla.focus.helpers.TestAssetHelper.getGenericTabAsset
import org.mozilla.focus.helpers.TestAssetHelper.getPlainPageAsset
import org.mozilla.focus.helpers.TestHelper
import org.mozilla.focus.helpers.TestHelper.clickSnackBarActionButton
import org.mozilla.focus.helpers.TestHelper.getStringResource
import org.mozilla.focus.helpers.TestHelper.verifySnackBarText
import org.mozilla.focus.testAnnotations.SmokeTest

/**
 * Open multiple sessions and verify that the trash icon changes to a tabs counter
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class MultitaskingTest {
    private lateinit var webServer: MockWebServer
    private val store = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .applicationContext
        .components
        .store
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get: Rule
    var mActivityTestRule = MainActivityFirstrunTestRule(showFirstRun = false)

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    @Throws(Exception::class)
    fun startWebServer() {
        featureSettingsHelper.setCfrForTrackingProtectionEnabled(false)
        featureSettingsHelper.setNumberOfTabsOpened(4)
        webServer = MockWebServer().apply {
            dispatcher = MockWebServerHelper.AndroidAssetDispatcher()
            start()
        }
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        webServer.shutdown()
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @Ignore("Failing, see: https://github.com/mozilla-mobile/focus-android/issues/6709")
    @SmokeTest
    @Test
    fun testVisitingMultipleSites() {
        val tab1 = getGenericTabAsset(webServer, 1)
        val tab2 = getGenericTabAsset(webServer, 2)
        val tab3 = getGenericTabAsset(webServer, 3)
        val eraseBrowsingSnackBarText = getStringResource(R.string.feedback_erase2)
        val customTabPage = getPlainPageAsset(webServer)

        // Load website: Erase button visible, Tabs button not
        searchScreen {
        }.loadPage(tab1.url) {
            verifyPageContent("Tab 1")
            longPressLink("Tab 2")
            verifyLinkContextMenu(tab2.url)
            openLinkInNewTab()
            verifyNumberOfTabsOpened(2)
            longPressLink("Tab 3")
            openLinkInNewTab()
            verifySnackBarText("New private tab opened")
            clickSnackBarActionButton("SWITCH")
            verifyNumberOfTabsOpened(3)
        }

        launchActivity<IntentReceiverActivity>(TestHelper.createCustomTabIntent(customTabPage.url))

        customTab {
            progressBar.waitUntilGone(TestHelper.waitingTime)
            verifyPageURL(customTabPage.url)
            openCustomTabMenu()
        }.clickOpenInFocusButton() {
            verifyNumberOfTabsOpened(4)
        }.openTabsTray {
            verifyTabsOrderAndCorespondingCloseButton(tab1.title, tab3.title, tab2.title, customTabPage.title)
        }.selectTab(tab1.title) {
            verifyPageContent("Tab 1")
        }.clearBrowsingData {
            verifySnackBarText(eraseBrowsingSnackBarText)
            assertTrue(store.state.privateTabs.isEmpty())
        }
    }

    @Ignore("Failing, see: https://github.com/mozilla-mobile/focus-android/issues/6708")
    @SmokeTest
    @Test
    fun closeTabButtonTest() {
        val tab1 = getGenericTabAsset(webServer, 1)
        val tab2 = getGenericTabAsset(webServer, 2)
        val tab3 = getGenericTabAsset(webServer, 3)

        searchScreen {
        }.loadPage(tab1.url) {
            verifyPageContent("Tab 1")
            longPressLink("Tab 2")
            openLinkInNewTab()
            longPressLink("Tab 3")
            openLinkInNewTab()
            verifyNumberOfTabsOpened(3)
        }.openTabsTray {
            verifyTabsOrderAndCorespondingCloseButton(tab1.title, tab3.title, tab2.title)
        }.closeTab(tab1.title) {
        }.openTabsTray {
            verifyTabsOrderAndCorespondingCloseButton(tab3.title, tab2.title)
        }.closeTab(tab3.title) {
            verifyTabsCounterNotShown()
        }
    }
}
