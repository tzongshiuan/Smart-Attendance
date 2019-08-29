package com.gorilla.attendance.ui.screenSaver

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.gorilla.attendance.R
import com.gorilla.attendance.ui.common.FetchingIdlingResource
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.TestTool
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/13
 * Description:
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ScreenSaverFragmentTest {

    @get:Rule
    val mActivityRule = ActivityTestRule(MainActivity::class.java)

    private lateinit var mActivity: MainActivity

    private lateinit var fetchIdling: FetchingIdlingResource

    @Before
    fun setUp() {
        mActivity = mActivityRule.activity

        fetchIdling = FetchingIdlingResource()
        IdlingRegistry.getInstance().register(fetchIdling)
        mActivity.mPreferences.fetcherListener = fetchIdling

        MainActivity.IS_DEBUG_GOGO = true
        MainActivity.GET_DEVICE_IDENTITIES = false
        MainActivity.IS_ENABLE_SCREEN_SAVER = true

        /**
         * Extend idling resource timeout to 300 seconds
         */
//        IdlingPolicies.setMasterPolicyTimeout(300L, TimeUnit.SECONDS)
//        IdlingPolicies.setIdlingResourceTimeout(300L, TimeUnit.SECONDS)
    }

    /**
     * screenSaverTest
     */
    @Test
    fun screenSaverTest() {
        Timber.d("[UnitTest][screenSaverTest] *** Start ***")

        // open screen saver in the setting page
        TestTool.enterSettingFragment(fetchIdling, 1)
        onView(withId(R.id.idleTimeEditText)).perform(TestTool.setTextInEditText("30"))
        blockUI(500)
        onView(withId(R.id.screenSwitch)).perform(scrollTo())
        blockUI(500)
        onView(withId(R.id.screenSwitch)).perform(TestTool.setSwitchEnable(true))
        blockUI(500)
        // save and apply
        onView(withId(R.id.btnRight)).perform(click())
        Timber.d("[UnitTest] change setting success")

        fetchIdling.beginFetching()
        onView(withId(R.id.imgEmployee)).perform(click())
        Timber.d("[UnitTest] navigate to ChangeModeFragment and wait screen saver start")

        blockUI(35000)
        Timber.d("[UnitTest] navigate to ScreenSaverFragment success")

        Timber.d("[UnitTest][screenSaverTest] *** End ***")
        blockUI(3000)
    }

    @After
    fun setUpAfter() {
        fetchIdling.doneFetching()
        IdlingRegistry.getInstance().unregister(fetchIdling)
    }

    private fun blockUI(millis: Long) {
        Thread.sleep(millis)
    }
}