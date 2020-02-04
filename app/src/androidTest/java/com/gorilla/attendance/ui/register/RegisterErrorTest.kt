package com.gorilla.attendance.ui.register

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
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
import java.util.*


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/13
 * Description:
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RegisterErrorTest {

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
    }

    /**
     * registerFormErrorTest
     */
    @Test
    fun securityRegistrationTest() {
        Timber.d("[UnitTest][registerFormErrorTest] *** Start ***")

        // change to registration mode first
        TestTool.enterSettingFragment(fetchIdling, 1)
        onView(withId(R.id.appModeSpinner)).perform(scrollTo())
        blockUI(500)
        onView(withId(R.id.appModeSpinner)).perform(TestTool.setSpinnerSelectedIndex(0))
        blockUI(500)
        // save and apply
        onView(withId(R.id.btnRight)).perform(click())


        TestTool.enterSpecifiedMode(fetchIdling, TestTool.MEMBER_EMPLOYEE, TestTool.MODE_SECURITY_CODE)
        Timber.d("[UnitTest] navigate to SecurityCodeFragment success")

        val random = Random().nextInt(10000 - 10) + 10

        // employee id error test (empty)
        clickBtnRight()
        Timber.d("[UnitTest] test empty employee ID success")
        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.idEditText,
            "${TestTool.TEST_EMPLOYEE_ID}_$random"))
        blockUI(500)

        // employee name error test (empty)
        clickBtnRight()
        Timber.d("[UnitTest] test empty employee name success")
        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.nameEditText,
            "${TestTool.TEST_EMPLOYEE_NAME}_$random"))
        blockUI(500)

        // employee email error test (empty)
        clickBtnRight()
        Timber.d("[UnitTest] test empty email success")
        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.emailEditText,
            "${TestTool.TEST_EMPLOYEE_EMAIL}_$random"))
        blockUI(500)

        // employee password error test (empty and wrong format)
        onView(withId(R.id.passwordEditText)).perform(scrollTo())
        blockUI(100)

        clickBtnRight()
        Timber.d("[UnitTest] test empty password success")

        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.passwordEditText,
            "12345678_$random"))
        blockUI(500)
        clickBtnRight()
        Timber.d("[UnitTest] test invalid format password success")

        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.passwordEditText,
            "${TestTool.TEST_EMPLOYEE_PASSWORD}_$random"))
        blockUI(500)

        // employee security error test (empty and invalid)
        onView(withId(R.id.employeeSecurityEditText)).perform(scrollTo())
        blockUI(100)

        clickBtnRight()
        Timber.d("[UnitTest] test empty security code success")

        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.employeeSecurityEditText,
            TestTool.TEST_SECURITY_CODE
        ))
        blockUI(500)
        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.clickChildWithId(R.id.checkBtn))
        blockUI(2000)
        Timber.d("[UnitTest] test invalid security code success")

        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.setChildEditTextWithId(R.id.employeeSecurityEditText,
            "${TestTool.TEST_SECURITY_CODE}_$random"))
        blockUI(500)
        onView(withId(R.id.employeeRegisterForm)).perform(TestTool.clickChildWithId(R.id.checkBtn))
        blockUI(2000)
        Timber.d("[UnitTest] test valid security code success")


        Timber.d("[UnitTest][registerFormErrorTest] *** End ***")
        blockUI(3000)
    }

    private fun clickBtnRight() {
        onView(withId(R.id.btnRight)).perform(click())
        blockUI(2000)
        onView(withId(R.id.messageText)).perform(click())
        blockUI(500)
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