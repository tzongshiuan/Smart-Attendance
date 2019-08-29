package com.gorilla.attendance.ui.setting

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.gorilla.attendance.R
import com.gorilla.attendance.ui.common.FetchingIdlingResource
import com.gorilla.attendance.ui.main.MainActivity
import com.gorilla.attendance.utils.TestTool
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/7/5
 * Description:
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingFragmentTest {

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
        MainActivity.IS_SKIP_FDR = true
        MainActivity.USE_TEST_FACE = true
        MainActivity.SEND_CLOCK_EVENT = false
        MainActivity.GET_DEVICE_IDENTITIES = true
        MainActivity.IS_ENABLE_SCREEN_SAVER = false
    }

    @Test
    fun enterSettingFragmentTest() {
        Timber.d("[UnitTest][enterSettingFragmentTest] *** Start ***")

        fetchIdling.beginFetching()

        // test navigate
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] navigate to SettingFragment success")

        // test verification
        onView(withId(R.id.settingAccountEditText)).perform(TestTool.setTextInEditText("account"))
        onView(withId(R.id.settingPasswordEditText)).perform(TestTool.setTextInEditText("password"))
        blockUI(1000)
        onView(withId(R.id.btnRight)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] test verification failed UI")

        onView(withId(R.id.settingAccountEditText)).perform(TestTool.setTextInEditText("aa"))
        onView(withId(R.id.settingPasswordEditText)).perform(TestTool.setTextInEditText("aa"))
        blockUI(1000)
        onView(withId(R.id.btnRight)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] test verification success")

        // test option item switch
        onView(withId(R.id.settingOptionsView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        blockUI(500)
        Timber.d("[UnitTest] change setting option to position 1 success")

        onView(withId(R.id.settingOptionsView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))
        blockUI(500)
        Timber.d("[UnitTest] change setting option to position 2 success")

        onView(withId(R.id.settingOptionsView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()))
        blockUI(500)
        Timber.d("[UnitTest] change setting option to position 3 success")

        // back
        onView(withId(R.id.btnLeft)).perform(click())

        Timber.d("[UnitTest][enterSettingFragmentTest] *** End ***")
        blockUI(2000)
    }

    @Test
    fun changeApplicationModeTest() {
        Timber.d("[UnitTest][changeApplicationModeTest] *** Start ***")

        // select verification mode
        TestTool.enterSettingFragment(fetchIdling, 1)
        onView(withId(R.id.appModeSpinner)).perform(scrollTo())
        blockUI(500)
        onView(withId(R.id.appModeSpinner)).perform(TestTool.setSpinnerSelectedIndex(1))
        blockUI(500)

        // save and apply
        onView(withId(R.id.btnRight)).perform(click())


        // select registration mode
        TestTool.enterSettingFragment(fetchIdling, 1)
        onView(withId(R.id.appModeSpinner)).perform(scrollTo())
        blockUI(500)
        onView(withId(R.id.appModeSpinner)).perform(TestTool.setSpinnerSelectedIndex(0))
        blockUI(500)
        Timber.d("[UnitTest] change application mode to 0 success")

        // save and apply
        onView(withId(R.id.btnRight)).perform(click())


        // select verification mode
        TestTool.enterSettingFragment(fetchIdling, 1)
        onView(withId(R.id.appModeSpinner)).perform(scrollTo())
        blockUI(500)
        onView(withId(R.id.appModeSpinner)).perform(TestTool.setSpinnerSelectedIndex(1))
        blockUI(500)
        Timber.d("[UnitTest] change application mode to 1 success")


        // save and apply
        onView(withId(R.id.btnRight)).perform(click())

        Timber.d("[UnitTest][changeApplicationModeTest] *** End ***")
        blockUI(2000)
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