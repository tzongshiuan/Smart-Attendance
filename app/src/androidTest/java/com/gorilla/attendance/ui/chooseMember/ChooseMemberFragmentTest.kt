package com.gorilla.attendance.ui.chooseMember

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.gorilla.attendance.R
import com.gorilla.attendance.ui.common.FetchingIdlingResource
import com.gorilla.attendance.ui.main.MainActivity
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
class ChooseMemberFragmentTest {

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
    }

    @Test
    fun chooseMemberTest() {
        Timber.d("[UnitTest][chooseMemberTest] *** Start ***")

        fetchIdling.beginFetching()

        onView(withId(R.id.imgEmployee)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] navigate to ChooseModeFragment(employee) success")

        // back
        pressBack()
        blockUI(2000)
        Timber.d("[UnitTest] back to ChooseMemberFragment success")

        onView(withId(R.id.imgVisitor)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] navigate to ChooseModeFragment(visitor) success")

        // back
        pressBack()
        blockUI(2000)
        Timber.d("[UnitTest] back to ChooseMemberFragment success")

        Timber.d("[UnitTest][chooseMemberTest] *** End ***")
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