package com.gorilla.attendance.utils

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import com.gorilla.attendance.R
import com.gorilla.attendance.ui.common.FetchingIdlingResource
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import timber.log.Timber


/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/6
 * Description:
 */
object TestTool {
    /**
     * Specified test data
     */
    const val TEST_RFID = "DBCAC60C"    // must exist and valid in the portal
    const val TEST_SECURITY_CODE = "07121234"   // must exist and valid in the portal
    const val TEST_EMPLOYEE_ID = "hsuan"
    const val TEST_EMPLOYEE_NAME = "Tsung Hsuan, Lai"
    const val TEST_EMPLOYEE_EMAIL = "tsunghsuanlai@gorilla-technology.com"
    const val TEST_EMPLOYEE_PASSWORD = "Shiuan6391@0606"



    const val MEMBER_EMPLOYEE = 0
    const val MEMBER_VISITOR = 1

    const val MODE_RFID = 0
    const val MODE_SECURITY_CODE = 1
    const val MODE_QR_CODE = 2
    const val MODE_FACIAL = 3

    fun setConstraintVisibility(value: Boolean): ViewAction {
        return object: ViewAction {

            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(ConstraintLayout::class.java)
            }

            override fun perform(uiController: UiController, view: View) {
                view.visibility = if (value) View.VISIBLE else View.GONE
            }

            override fun getDescription(): String {
                return "Show/Hide View"
            }
        }
    }

    fun setTextInEditText(text: String): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
            }

            override fun perform(uiController: UiController?, view: View?) {
                (view as EditText).setText(text)
            }

            override fun getDescription(): String {
                return "replace text"
            }
        }
    }

    fun setSpinnerSelectedIndex(index: Int): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), isAssignableFrom(Spinner::class.java))
            }

            override fun perform(uiController: UiController?, view: View?) {
                (view as Spinner).setSelection(index)
            }

            override fun getDescription(): String {
                return "change selected index"
            }
        }
    }

    fun setChildEditTextWithId(id: Int, text: String): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun perform(uiController: UiController?, view: View?) {
                val v = view?.findViewById<EditText>(id)
                v?.setText(text)
            }

            override fun getDescription(): String {
                return "set child edit text with specified id"
            }
        }
    }

    fun clickChildWithId(id: Int): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun perform(uiController: UiController?, view: View?) {
                val v = view?.findViewById<Button>(id)
                v?.performClick()
            }

            override fun getDescription(): String {
                return "click child view"
            }
        }
    }

    fun setSwitchEnable(enable: Boolean): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isDisplayed()
            }

            override fun perform(uiController: UiController?, view: View?) {
                (view as Switch).isChecked = enable
            }

            override fun getDescription(): String {
                return "set switch is enable or not"
            }
        }
    }

    // click item in ListView
//    onData(Matchers.allOf())
//    .inAdapterView(withId(R.id.photo_gridview)) // listview的id
//    .atPosition(1)                              // 所在位置
//    .onChildView(withId(R.id.imageview_photo))  // item中子控件id
//    .perform(click());

//    fun getText(matcher: Matcher<View>): String {
//        val textStr = arrayOf<String>(null)
//        onView(matcher).perform(object : ViewAction {
//            override fun getConstraints(): Matcher<View> {
//                return isAssignableFrom(TextView::class.java)
//            }
//
//            override fun getDescription(): String {
//                return "get text from TextView"
//            }
//
//            override fun perform(uiController: UiController, view: View) {
//                textStr[0] = (view as TextView).getText().toString()
//            }
//        })
//        return textStr[0]
//    }

//    fun changeTextColor(matcher: Matcher<View>) {
//        onView(matcher).perform(object : ViewAction {
//            override fun getConstraints(): Matcher<View> {
//                return isAssignableFrom(TextView::class.java)
//            }
//
//            override fun getDescription(): String {
//                return "Change text Color"
//            }
//
//            override fun perform(uiController: UiController, view: View) {
//                (view as TextView).setTextColor(Color.parseColor("#ff0000"))
//            }
//        })
//    }

    /**
     * Enter to setting fragment and do verification automatically
     */
    fun enterSettingFragment(fetchIdling: FetchingIdlingResource, optionPosition: Int = -1, isBeginFetching: Boolean = true) {
        if (isBeginFetching) {
            fetchIdling.beginFetching()
        }

        // navigate to setting fragment
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        onView(withId(R.id.timeText)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] navigate to SettingFragment success")

        // verification setting account
        onView(withId(R.id.settingAccountEditText)).perform(setTextInEditText("aa"))
        onView(withId(R.id.settingPasswordEditText)).perform(setTextInEditText("aa"))
        blockUI(1000)
        onView(withId(R.id.btnRight)).perform(click())
        blockUI(2000)
        Timber.d("[UnitTest] setting account verification success")

        if (optionPosition != -1) {
            onView(withId(R.id.settingOptionsView)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(optionPosition, click()))
            blockUI(1000)
            Timber.d("[UnitTest] change setting option to position 1 success")
        }
    }

    /**
     * Enter to specified mode
     */
    fun enterSpecifiedMode(fetchIdling: FetchingIdlingResource, member: Int, mode: Int) {
        fetchIdling.beginFetching()

        when (member) {
            MEMBER_EMPLOYEE -> onView(withId(R.id.imgEmployee)).perform(click())
            MEMBER_VISITOR -> onView(withId(R.id.imgVisitor)).perform(click())
        }
        Timber.d("[UnitTest] navigate to ChangeModeFragment success")
        blockUI(1000)

        when (mode) {
            MODE_RFID -> onView(withId(R.id.imgIdCard)).perform(click())
            MODE_SECURITY_CODE -> onView(withId(R.id.imgSecurityCode)).perform(click())
            MODE_QR_CODE -> onView(withId(R.id.imgQrCode)).perform(click())
            MODE_FACIAL -> onView(withId(R.id.imgFacial)).perform(click())
        }
        Timber.d("[UnitTest] navigate to specified mode success")
        blockUI(1000)
    }

    private fun blockUI(millis: Long) {
        Thread.sleep(millis)
    }
}