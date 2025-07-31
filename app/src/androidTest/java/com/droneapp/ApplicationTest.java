package com.droneapp;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Simple instrumentation test that verifies the application context.
 *
 * <p>
 * The original ApplicationTest class supplied by the user could not be accessed in
 * this environment.  To ensure the test infrastructure remains intact, this
 * placeholder test confirms that the package name is correct.  Additional
 * instrumentation tests can be added here as needed.
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.droneapp", appContext.getPackageName());
    }
}