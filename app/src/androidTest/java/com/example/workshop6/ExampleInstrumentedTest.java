// Contributor(s): Owen
// Main: Owen - Android app UI and API integration.

package com.example.workshop6;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

// Keeps the on-device instrumented test task wired. Replace with UI flows when you add Espresso coverage.
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Target package under test for Gradle connected checks
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.workshop6", appContext.getPackageName());
    }
}