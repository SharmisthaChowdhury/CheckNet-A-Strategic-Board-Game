
package com.example.bluetoothconnection;
import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device. 
 *
 * @see <a href="http://d.android.com/tools/testing">Testing 
documentation</a> 
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest1 {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test. 
        Instrumentation InstrumentationRegistry = new Instrumentation();
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.sarthitechnology.btchat",
                appContext.getPackageName());
    }
}