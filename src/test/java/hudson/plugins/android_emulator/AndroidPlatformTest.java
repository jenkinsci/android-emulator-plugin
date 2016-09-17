package hudson.plugins.android_emulator;

import hudson.plugins.android_emulator.model.AndroidPlatform;
import junit.framework.TestCase;

public class AndroidPlatformTest extends TestCase {

    public void testCreateKnownPlatform() {
        assertEquals(AndroidPlatform.SDK_1_6, AndroidPlatform.valueOf("1.6"));
        assertEquals(AndroidPlatform.SDK_1_6, AndroidPlatform.valueOf("4"));
        assertEquals(AndroidPlatform.SDK_1_6, AndroidPlatform.valueOf("android-4"));
    }

    public void testCreateAddonPlatform() {
        final String name = "Some:Addon:11";
        AndroidPlatform platform = AndroidPlatform.valueOf(name);
        assertEquals(name, platform.getTargetName());
    }

    public void testCreateInvalidPlatform() {
        assertNull(AndroidPlatform.valueOf(null));

        final String name = "iOS 6.1";
        AndroidPlatform platform = AndroidPlatform.valueOf(name);
        assertEquals(name, platform.getTargetName());
        assertEquals(-1, platform.getSdkLevel());
    }

    public void testGetSdkLevel() {
        assertEquals(9, AndroidPlatform.SDK_2_3.getSdkLevel());
        assertEquals(10, AndroidPlatform.valueOf("2.3.3").getSdkLevel());
        assertEquals(11, AndroidPlatform.valueOf("Some:Addon:11").getSdkLevel());
    }

    public void testIsCustomPlatform() {
        assertFalse(AndroidPlatform.SDK_2_3.isCustomPlatform());
        assertTrue(AndroidPlatform.valueOf("Some:Addon:12").isCustomPlatform());
    }

    public void testRequiresAbi() {
        assertFalse(AndroidPlatform.SDK_1_1.requiresAbi());
        assertTrue(AndroidPlatform.SDK_2_3_3.requiresAbi()); // See JENKINS-14741 & commit 485d72b
        assertFalse(AndroidPlatform.SDK_3_2.requiresAbi());
        assertFalse(AndroidPlatform.SDK_4_0.requiresAbi());
        assertTrue(AndroidPlatform.SDK_4_0_3.requiresAbi());
        assertTrue(AndroidPlatform.SDK_4_2.requiresAbi());
        assertTrue(AndroidPlatform.valueOf("android-99").requiresAbi());
        assertTrue(AndroidPlatform.valueOf("Intel Atom x86 System Image").requiresAbi());
    }

}
