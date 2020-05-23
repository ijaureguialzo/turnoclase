package com.jaureguialzo.turnoclaseprofesor;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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

        CleanStatusBar.enableWithDefaults();

        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        ViewInteraction botonSiguiente = onView(allOf(withId(R.id.botonEnCola), isDisplayed()));
        ViewInteraction botonCodigoAula = onView(allOf(withId(R.id.botonCodigoAula), isDisplayed()));

        botonCodigoAula.perform(click());
        Screengrab.screenshot("00-NuevaAula");

        botonSiguiente.perform(click());
        Screengrab.screenshot("01-Quedan2");

        botonSiguiente.perform(click());
        Screengrab.screenshot("02-Quedan1");

        botonSiguiente.perform(click());
        Screengrab.screenshot("03-Quedan0");

        botonSiguiente.perform(click());
        Screengrab.screenshot("04-Terminado");

        openActionBarOverflowOrOptionsMenu(mActivityTestRule.getActivity());
        Screengrab.screenshot("05-Menu");

        CleanStatusBar.disable();
    }

}
