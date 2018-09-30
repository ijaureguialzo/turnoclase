package com.jaureguialzo.turnoclaseprofesor;

import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Test
    public void mainActivityTest() {

        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        ViewInteraction botonSiguiente = onView(allOf(withId(R.id.botonEnCola), isDisplayed()));

        Screengrab.screenshot("01-Quedan2");
        botonSiguiente.perform(click());

        Screengrab.screenshot("02-Quedan1");
        botonSiguiente.perform(click());

        Screengrab.screenshot("03-Quedan0");
        botonSiguiente.perform(click());

        Screengrab.screenshot("04-Terminado");

    }

}
