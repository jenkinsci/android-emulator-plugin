package hudson.plugins.android_emulator;

import hudson.plugins.android_emulator.sdk.Tool;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmulatorConfigTest {

    @Test // JENKINS-26338
    public void shouldSelectExecutor64WhenPassedAsExecutorAndAvdIsSelected() {
        EmulatorConfig emulatorConfigWithAvdName =
                EmulatorConfig.create("hudson_en-US_160_WVGA_android-21", "5.0", "160", "WVGA", "", "", false, false,
                        false, "", "", "", "", "emulator64-arm", "");
        assertEquals(Tool.EMULATOR64_ARM, emulatorConfigWithAvdName.getExecutable());
    }

    @Test
    public void shouldSelectExecutor64WhenPassedAsExecutorAndAvdIsEmpty() {
        EmulatorConfig emulatorConfigWithNoAvdName =
                EmulatorConfig.create("", "5.0", "160", "WVGA", "", "", false, false, false, "", "", "", "",
                        "emulator64-arm", "");
        assertEquals(Tool.EMULATOR64_ARM, emulatorConfigWithNoAvdName.getExecutable());
    }

}
