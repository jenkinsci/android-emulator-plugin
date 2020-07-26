package hudson.plugins.android_emulator.sdk;

import junit.framework.TestCase;

import java.io.IOException;

import org.junit.Test;

public class AndroidSdkTest extends TestCase {

    @Test
    public void testGetSdkToolsMajorVersion() throws Exception {
        assertEquals(0, createSdkWithTools(null).getSdkToolsMajorVersion());
        assertEquals(20, createSdkWithTools("20").getSdkToolsMajorVersion());
        assertEquals(20, createSdkWithTools("20.0").getSdkToolsMajorVersion());
        assertEquals(20, createSdkWithTools("20.0.1").getSdkToolsMajorVersion());
        assertEquals(21, createSdkWithTools("21 rc3").getSdkToolsMajorVersion());
        assertEquals(22, createSdkWithTools("22.6").getSdkToolsMajorVersion());
    }

    @Test
    public void testEmulatorEngineV2Support() {
        assertEqualsHelperEmulatorEngineV2Support(null, false, false);
        assertEqualsHelperEmulatorEngineV2Support("24", false, false);
        assertEqualsHelperEmulatorEngineV2Support("24.1.1", false, false);
        assertEqualsHelperEmulatorEngineV2Support("24.4", false, false);
        assertEqualsHelperEmulatorEngineV2Support("25", true, false);
        assertEqualsHelperEmulatorEngineV2Support("25.2.1", true, false);
        assertEqualsHelperEmulatorEngineV2Support("25.3", true, false);
        assertEqualsHelperEmulatorEngineV2Support("26", true, true);
        assertEqualsHelperEmulatorEngineV2Support("26.1.1", true, true);
        assertEqualsHelperEmulatorEngineV2Support("27", true, true);
    }

    @Test
    public void testUseLegacySdkStructure() {
        assertFalse(createSdkWithTools(null).useLegacySdkStructure());
        assertTrue(createSdkWithTools("24").useLegacySdkStructure());
        assertTrue(createSdkWithTools("24.1.1").useLegacySdkStructure());
        assertTrue(createSdkWithTools("24.4").useLegacySdkStructure());
        assertTrue(createSdkWithTools("25").useLegacySdkStructure());
        assertTrue(createSdkWithTools("25.2.1").useLegacySdkStructure());
        assertFalse(createSdkWithTools("25.3").useLegacySdkStructure());
        assertFalse(createSdkWithTools("26").useLegacySdkStructure());
        assertFalse(createSdkWithTools("26.1.1").useLegacySdkStructure());
        assertFalse(createSdkWithTools("27").useLegacySdkStructure());
    }

    private void assertEqualsHelperEmulatorEngineV2Support(final String sdkToolsVersion,
            final boolean expectedSupportV2Result, final boolean expectedSupportV2FullResult) {

        final AndroidSdk androidSdk = createSdkWithTools(sdkToolsVersion);
        assertEquals(expectedSupportV2Result, androidSdk.supportsEmulatorV2());
        assertEquals(expectedSupportV2FullResult, androidSdk.supportsEmulatorV2Full());
    }

    private static AndroidSdk createSdkWithTools(String version) {
        AndroidSdk sdk = null;
        try {
            sdk = new AndroidSdk(null, null);
            sdk.setSdkToolsVersion(version);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sdk;
    }

}
