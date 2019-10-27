package com.jaureguialzo.turnoclase;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.FalconScreenshotStrategy;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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

        Screengrab.setDefaultScreenshotStrategy(new FalconScreenshotStrategy(mActivityTestRule.getActivity()));

        ViewInteraction editText = onView(allOf(withId(R.id.campoAula), withText("BE131"), isDisplayed()));
        editText.check(matches(withText("BE131")));

        Screengrab.screenshot("01-PantallaLogin");

        ViewInteraction appCompatButton = onView(allOf(withId(R.id.botonSiguiente), isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction textView = onView(allOf(withId(R.id.etiquetaAula), isDisplayed()));
        textView.check(matches(withText("BE131")));

        ViewInteraction botonActualizar = onView(allOf(withId(R.id.botonActualizar), isDisplayed()));

        botonActualizar.perform(click());
        ViewInteraction etiquetaNumero = onView(allOf(withId(R.id.etiquetaMensaje), withText("2"), isDisplayed()));
        etiquetaNumero.check(matches(withText("2")));
        Screengrab.screenshot("02-Faltan2");

        botonActualizar.perform(click());
        etiquetaNumero = onView(allOf(withId(R.id.etiquetaMensaje), withText("1"), isDisplayed()));
        etiquetaNumero.check(matches(withText("1")));
        Screengrab.screenshot("03-Faltan1");

        botonActualizar.perform(click());
        Screengrab.screenshot("04-EsTuTurno");

        botonActualizar.perform(click());
        Screengrab.screenshot("05-Terminado");

    }

}
