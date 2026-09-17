package com.yuvraj.openchatai

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the app launches and reaches the resumed state without crashing. */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @Test
    fun mainActivityLaunches() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { }
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
