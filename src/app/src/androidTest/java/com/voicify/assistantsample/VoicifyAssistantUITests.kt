package com.voicify.assistantsample

import android.annotation.SuppressLint
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.TreeIterables
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@RunWith(AndroidJUnit4::class)
class VoicifyAssistantUITests {
    @Rule
    @JvmField var activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.voicify.assistantsample", appContext.packageName)
    }

    @Test
    fun testPlayEffect() {
        onView(withId(R.id.assistantMic)).perform(click())
        onView(withId(R.id.inputTextMessage)).perform(click())
        onView(withId(R.id.inputTextMessage)).perform(typeText("play how we got here"))
        onView(withId(R.id.sendMessageButtonImageView)).perform(click())
        onView(withId(R.id.speakTextView))
        EspressoExtensions.BaseRobot().assertOnView(withIndex(withId(R.id.messageTextView),1), matches(hasValueEqualTo("Here's how we got here by explosive ear candy")))
        EspressoExtensions.BaseRobot().assertOnView(withIndex(withId(R.id.nowPlayingTextView),1), matches(hasValueEqualTo("Now playing How We Got Here")))
    }

    @Test
    fun testSpeakAndTypeUI(){
        onView(withId(R.id.assistantMic)).perform(click())
        onView(withId(R.id.speakTextView)).check(matches(hasValueEqualTo("SPEAK")))
        onView(withId(R.id.assistantStateTextView)).check(matches(hasValueEqualTo("Listening...")))
        onView(withId(R.id.drawerWelcomeTextView)).check(matches(hasValueEqualTo("How can i help?")))
        onView(withId(R.id.inputTextMessage)).perform(click())
        onView(withId(R.id.assistantStateTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.spokenTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.speakingAnimation)).check(matches(not(isDisplayed())))
        onView(withId(R.id.typeTextView)).check(matches(hasValueEqualTo("TYPE")))
    }

    fun withIndex(matcher: Matcher<View?>, index: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var currentIndex = 0
            var viewObjHash = 0

            @SuppressLint("DefaultLocale")
            override fun describeTo(description: Description) {
                description.appendText(String.format("with index: %d ", index))
                matcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                if (matcher.matches(view) && currentIndex++ == index) {
                    viewObjHash = view.hashCode()
                }
                return view.hashCode() == viewObjHash
            }
        }
    }

    fun hasValueEqualTo(content: String): Matcher<View?>? {
        return object : TypeSafeMatcher<View?>() {
            override  fun describeTo(description: Description) {
                description.appendText("Has EditText/TextView the value:  $content")
            }

            override  fun matchesSafely(view: View?): Boolean {
                if (view !is TextView && view !is EditText) {
                    return false
                }
                if (view != null) {
                    val text: String
                    text = if (view is TextView) {
                        view.text.toString()
                    } else {
                        (view as EditText).text.toString()
                    }
                    return text.equals(content, ignoreCase = true)
                }
                return false
            }
        }
    }
}

class EspressoExtensions {

    open class BaseRobot {

        fun doOnView(matcher: Matcher<View>, vararg actions: ViewAction) {
            actions.forEach {
                waitForView(matcher).perform(it)
            }
        }

        fun assertOnView(matcher: Matcher<View>, vararg assertions: ViewAssertion) {
            assertions.forEach {
                waitForView(matcher).check(it)
            }
        }

        /**
         * Perform action of implicitly waiting for a certain view.
         * This differs from EspressoExtensions.searchFor in that,
         * upon failure to locate an element, it will fetch a new root view
         * in which to traverse searching for our @param match
         *
         * @param viewMatcher ViewMatcher used to find our view
         */
        fun waitForView(
            viewMatcher: Matcher<View>,
            waitMillis: Int = 5000,
            waitMillisPerTry: Long = 100
        ): ViewInteraction {

            // Derive the max tries
            val maxTries = waitMillis / waitMillisPerTry.toInt()

            var tries = 0

            for (i in 0..maxTries)
                try {
                    // Track the amount of times we've tried
                    tries++

                    // Search the root for the view
                    onView(isRoot()).perform(searchFor(viewMatcher))

                    // If we're here, we found our view. Now return it
                    return onView(viewMatcher)

                } catch (e: Exception) {

                    if (tries == maxTries) {
                        throw e
                    }
                    sleep(waitMillisPerTry)
                }

            throw Exception("Error finding a view matching $viewMatcher")
        }
    }

    companion object {

        /**
         * Perform action of waiting for a certain view within a single root view
         * @param matcher Generic Matcher used to find our view
         */
        fun searchFor(matcher: Matcher<View>): ViewAction {

            return object : ViewAction {

                override fun getConstraints(): Matcher<View> {
                    return isRoot()
                }

                override fun getDescription(): String {
                    return "searching for view $matcher in the root view"
                }

                override fun perform(uiController: UiController, view: View) {

                    var tries = 0
                    val childViews: Iterable<View> = TreeIterables.breadthFirstViewTraversal(view)

                    // Look for the match in the tree of childviews
                    childViews.forEach {
                        tries++
                        if (matcher.matches(it)) {
                            // found the view
                            return
                        }
                    }

                    throw NoMatchingViewException.Builder()
                        .withRootView(view)
                        .withViewMatcher(matcher)
                        .build()
                }
            }
        }
    }
}
