package hudson.plugins.android_emulator;

import java.util.Arrays;

import org.junit.Test;

import junit.framework.TestCase;

public class AndroidPlatformTest extends TestCase {

    @Test
    public void testValidPlatformString() {
        assertNotNull(AndroidPlatform.valueOf("2.3.3"));
        assertNotNull(AndroidPlatform.valueOf("Some:Addon:11"));

        assertNotNull(AndroidPlatform.valueOf("Google Inc.:Google APIs:23"));
        assertNotNull(AndroidPlatform.valueOf("Google Inc.:Google APIs:24"));
        assertNotNull(AndroidPlatform.valueOf("android-23"));
        assertNotNull(AndroidPlatform.valueOf("android-24"));
        assertNotNull(AndroidPlatform.valueOf("android-26"));

        assertNotNull(AndroidPlatform.valueOf("1.6"));
        assertNotNull(AndroidPlatform.valueOf("4"));
        assertNotNull(AndroidPlatform.valueOf("android-4"));

        assertNull(AndroidPlatform.valueOf("prefix:Comp:XXXX:4"));
        assertNull(AndroidPlatform.valueOf("Comp:4"));
    }

    @Test
    public void testCreateInvalidPlatform() {
        assertNull(AndroidPlatform.valueOf(null));

        final String name = "iOS 6.1";
        AndroidPlatform platform = AndroidPlatform.valueOf(name);
        assertEquals(name, platform.getTargetName());
        assertEquals(-1, platform.getSdkLevel());
    }

    @Test
    public void testGetSdkLevel() {
        assertEquals(4, AndroidPlatform.valueOf("1.6").getSdkLevel());
        assertEquals(4, AndroidPlatform.valueOf("4").getSdkLevel());
        assertEquals(4, AndroidPlatform.valueOf("android-4").getSdkLevel());
        assertEquals(9, AndroidPlatform.valueOf("2.3").getSdkLevel());
        assertEquals(10, AndroidPlatform.valueOf("2.3.3").getSdkLevel());
        assertEquals(11, AndroidPlatform.valueOf("Some:Addon:11").getSdkLevel());

        assertEquals(23, AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getSdkLevel());
        assertEquals(24, AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSdkLevel());
        assertEquals(23, AndroidPlatform.valueOf("android-23").getSdkLevel());
        assertEquals(24, AndroidPlatform.valueOf("android-24").getSdkLevel());
        assertEquals(26, AndroidPlatform.valueOf("android-26").getSdkLevel());
        assertEquals(26, AndroidPlatform.valueOf("8.0").getSdkLevel());
        assertEquals(26, AndroidPlatform.valueOf("8.0.1").getSdkLevel());
        assertEquals(27, AndroidPlatform.valueOf("8.1").getSdkLevel());
        assertEquals(28, AndroidPlatform.valueOf("9.0").getSdkLevel());
        assertEquals(29, AndroidPlatform.valueOf("10.0").getSdkLevel());
        assertEquals(-1, AndroidPlatform.valueOf("11.0").getSdkLevel());
    }

    @Test
    public void testIsCustomPlatform() {
        assertFalse(AndroidPlatform.valueOf("2.3").isCustomPlatform());
        assertTrue(AndroidPlatform.valueOf("Some:Addon:12").isCustomPlatform());
        assertTrue(AndroidPlatform.valueOf("Google Inc.:Google APIs:23").isCustomPlatform());
        assertFalse(AndroidPlatform.valueOf("android-23").isCustomPlatform());
        assertTrue(AndroidPlatform.valueOf("Google Inc.:Google APIs:24").isCustomPlatform());
        assertFalse(AndroidPlatform.valueOf("android-24").isCustomPlatform());
        assertFalse(AndroidPlatform.valueOf("android-26").isCustomPlatform());
    }

    @Test
    public void testRequiresAbi() {
        assertFalse(AndroidPlatform.valueOf("1.1").requiresAbi());
        assertTrue(AndroidPlatform.valueOf("2.3.3").requiresAbi()); // See JENKINS-14741 & commit 485d72b
        assertFalse(AndroidPlatform.valueOf("3.2").requiresAbi());
        assertFalse(AndroidPlatform.valueOf("4.0").requiresAbi());
        assertTrue(AndroidPlatform.valueOf("4.0.3").requiresAbi());
        assertTrue(AndroidPlatform.valueOf("4.2").requiresAbi());
        assertTrue(AndroidPlatform.valueOf("android-99").requiresAbi());
        assertTrue(AndroidPlatform.valueOf("Intel Atom x86 System Image").requiresAbi());
    }

    @Test
    public void testName() {
        assertEquals("Google Inc.:Google APIs:23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getName());
        assertEquals("Google Inc.:Google APIs:24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getName());
        assertEquals("android-23", AndroidPlatform.valueOf("android-23").getName());
        assertEquals("android-24", AndroidPlatform.valueOf("android-24").getName());
        assertEquals("android-26", AndroidPlatform.valueOf("android-26").getName());
        assertEquals("2.3.3", AndroidPlatform.valueOf("2.3.3").getName());
        assertEquals("8.0", AndroidPlatform.valueOf("8.0").getName());
        assertEquals("26", AndroidPlatform.valueOf("26").getName());
    }

    @Test
    public void testTargetName() {
        assertEquals("Some:Addon:11", AndroidPlatform.valueOf("Some:Addon:11").getTargetName());
        assertEquals("Google Inc.:Google APIs:23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getTargetName());
        assertEquals("Google Inc.:Google APIs:24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getTargetName());
        assertEquals("Apple Inc.:Apple APIs:24", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getTargetName());
        assertEquals("android-23", AndroidPlatform.valueOf("android-23").getTargetName());
        assertEquals("android-24", AndroidPlatform.valueOf("android-24").getTargetName());
        assertEquals("android-26", AndroidPlatform.valueOf("android-26").getTargetName());
        assertEquals("android-10", AndroidPlatform.valueOf("2.3.3").getTargetName());
        assertEquals("android-26", AndroidPlatform.valueOf("8.0").getTargetName());
        assertEquals("android-26", AndroidPlatform.valueOf("26").getTargetName());
    }

    @Test
    public void testAndroidTargetName() {
        assertEquals("android-23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getAndroidTargetName());
        assertEquals("android-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getAndroidTargetName());
        assertEquals("android-24", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getAndroidTargetName());
        assertEquals("android-23", AndroidPlatform.valueOf("android-23").getAndroidTargetName());
        assertEquals("android-24", AndroidPlatform.valueOf("android-24").getAndroidTargetName());
        assertEquals("android-26", AndroidPlatform.valueOf("android-26").getAndroidTargetName());
        assertEquals("android-10", AndroidPlatform.valueOf("2.3.3").getAndroidTargetName());
        assertEquals("android-26", AndroidPlatform.valueOf("8.0").getAndroidTargetName());
        assertEquals("android-26", AndroidPlatform.valueOf("26").getAndroidTargetName());
    }

    @Test
    public void testPlatformName() {
        assertEquals("Google APIs", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getPlatformName());
        assertEquals("Google APIs", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPlatformName());
        assertEquals("Android API 23", AndroidPlatform.valueOf("android-23").getPlatformName());
        assertEquals("Android API 24", AndroidPlatform.valueOf("android-24").getPlatformName());
        assertEquals("Android API 26", AndroidPlatform.valueOf("android-26").getPlatformName());

        assertEquals("google_apis", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getPlatformId());
        assertEquals("google_apis", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPlatformId());
        assertEquals("default", AndroidPlatform.valueOf("android-23").getPlatformId());
        assertEquals("default", AndroidPlatform.valueOf("android-24").getPlatformId());
        assertEquals("default", AndroidPlatform.valueOf("android-26").getPlatformId());

        assertEquals("test_apis", AndroidPlatform.valueOf("Test Inc.:Test_APIs_:24").getPlatformId());
    }

    @Test
    public void testVendor() {
        assertEquals("Google Inc.", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getVendorName());
        assertEquals("Google Inc.", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getVendorName());
        assertEquals("", AndroidPlatform.valueOf("android-23").getVendorName());
        assertEquals("", AndroidPlatform.valueOf("android-24").getVendorName());
        assertEquals("", AndroidPlatform.valueOf("android-26").getVendorName());

        assertEquals("google", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getVendorId());
        assertEquals("google", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getVendorId());
        assertEquals("apple", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getVendorId());
        assertEquals("ms_company", AndroidPlatform.valueOf("MS Company:MS APIs:24").getVendorId());
        assertEquals("", AndroidPlatform.valueOf("android-23").getVendorId());
        assertEquals("", AndroidPlatform.valueOf("android-24").getVendorId());
        assertEquals("", AndroidPlatform.valueOf("android-26").getVendorId());
    }

    @Test
    public void testAddonName() {
        assertEquals("addon-google_apis-google-23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getAddonName());
        assertEquals("addon-google_apis-google-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getAddonName());
        assertEquals("addon-apple_apis-apple-99", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:99").getAddonName());
        assertEquals("addon-ms_apis-ms_company-24", AndroidPlatform.valueOf("MS Company:MS APIs:24").getAddonName());
        assertEquals("", AndroidPlatform.valueOf("android-23").getAddonName());
        assertEquals("", AndroidPlatform.valueOf("android-24").getAddonName());
        assertEquals("", AndroidPlatform.valueOf("android-26").getAddonName());
    }

    @Test
    public void testSystemImageName() {
        assertEquals("sys-img-x86-android-23", AndroidPlatform.valueOf("android-23").getSystemImageName("x86"));
        assertEquals("sys-img-x86-google_apis-23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getSystemImageName("x86"));
        assertEquals("sys-img-x86-google_apis-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageName("x86"));
        assertEquals("sys-img-x86_64-apple_apis-24", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getSystemImageName("x86_64"));
        assertEquals("sys-img-x86_64-ms_apis-24", AndroidPlatform.valueOf("MS Company:MS APIs:24").getSystemImageName("x86_64"));
        assertEquals("sys-img-armabi-v7a-android-23", AndroidPlatform.valueOf("android-23").getSystemImageName("armabi-v7a"));
        assertEquals("sys-img-armabi-v7a-android-24", AndroidPlatform.valueOf("android-24").getSystemImageName("armabi-v7a"));
        assertEquals("sys-img-arm64-v8a-android-26", AndroidPlatform.valueOf("android-26").getSystemImageName("arm64-v8a"));

        assertEquals("sys-img-x86-test-23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getSystemImageName("test/x86"));
        assertEquals("sys-img-x86-test-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageName("test/x86"));
        assertEquals("sys-img-x86_64-test-24", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getSystemImageName("test/x86_64"));
        assertEquals("sys-img-x86_64-test-24", AndroidPlatform.valueOf("MS Company:MS APIs:24").getSystemImageName("test/x86_64"));
        assertEquals("sys-img-armabi-v7a-test-23", AndroidPlatform.valueOf("android-23").getSystemImageName("test/armabi-v7a"));
        assertEquals("sys-img-armabi-v7a-test-24", AndroidPlatform.valueOf("android-24").getSystemImageName("test/armabi-v7a"));
        assertEquals("sys-img-arm64-v8a-test-26", AndroidPlatform.valueOf("android-26").getSystemImageName("test/arm64-v8a"));

        assertEquals("sys-img-x86-google_apis-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageName("/x86"));
        assertEquals("sys-img-x86-google_apis-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageName("///////x86"));

        assertEquals("sys-img-x86_64-google_apis-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageName("x86_64/"));
        assertEquals("sys-img-x86_64-google_apis-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageName("x86_64////"));
    }

    @Test
    public void testSystemImageNameLegacyFormat() {
        assertEquals("sysimg-23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getSystemImageNameLegacyFormat());
        assertEquals("sysimg-24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getSystemImageNameLegacyFormat());
        assertEquals("sysimg-24", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getSystemImageNameLegacyFormat());
        assertEquals("sysimg-24", AndroidPlatform.valueOf("MS Company:MS APIs:24").getSystemImageNameLegacyFormat());
        assertEquals("sysimg-23", AndroidPlatform.valueOf("android-23").getSystemImageNameLegacyFormat());
        assertEquals("sysimg-24", AndroidPlatform.valueOf("android-24").getSystemImageNameLegacyFormat());
        assertEquals("sysimg-26", AndroidPlatform.valueOf("android-26").getSystemImageNameLegacyFormat());
    }

    @Test
    public void testPackagePathOfSystemImage() {
        assertEquals("system-images;android-23;google_apis;x86", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getPackagePathOfSystemImage("x86"));
        assertEquals("system-images;android-24;google_apis;x86", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPackagePathOfSystemImage("x86"));
        assertEquals("system-images;android-24;apple_apis;x86_64", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getPackagePathOfSystemImage("x86_64"));
        assertEquals("system-images;android-24;ms_apis;x86_64", AndroidPlatform.valueOf("MS Company:MS APIs:24").getPackagePathOfSystemImage("x86_64"));
        assertEquals("system-images;android-23;default;armabi-v7a", AndroidPlatform.valueOf("android-23").getPackagePathOfSystemImage("armabi-v7a"));
        assertEquals("system-images;android-24;default;armabi-v7a", AndroidPlatform.valueOf("android-24").getPackagePathOfSystemImage("armabi-v7a"));
        assertEquals("system-images;android-26;default;arm64-v8a", AndroidPlatform.valueOf("android-26").getPackagePathOfSystemImage("arm64-v8a"));
        assertEquals("platforms;android-9", AndroidPlatform.valueOf("android-9").getPackagePathOfSystemImage("armabi-v7a"));
        assertEquals("system-images;android-10;default;armeabi-v7a", AndroidPlatform.valueOf("android-10").getPackagePathOfSystemImage("armeabi-v7a"));
        assertEquals("system-images;android-14;default;armeabi-v8a", AndroidPlatform.valueOf("android-14").getPackagePathOfSystemImage("armeabi-v8a"));
        assertEquals("platforms;android-13", AndroidPlatform.valueOf("android-13").getPackagePathOfSystemImage(null));

        assertEquals("system-images;android-23;test;x86", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getPackagePathOfSystemImage("test/x86"));
        assertEquals("system-images;android-24;google_apis;x86", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPackagePathOfSystemImage("x86"));
        assertEquals("system-images;android-24;apple_apis;x86_64", AndroidPlatform.valueOf("Apple Inc.:Apple APIs:24").getPackagePathOfSystemImage("x86_64"));
        assertEquals("system-images;android-24;test;x86_64", AndroidPlatform.valueOf("MS Company:MS APIs:24").getPackagePathOfSystemImage("test/x86_64"));
        assertEquals("system-images;android-23;test;armabi-v7a", AndroidPlatform.valueOf("android-23").getPackagePathOfSystemImage("test/armabi-v7a"));
        assertEquals("system-images;android-24;test;armabi-v7a", AndroidPlatform.valueOf("android-24").getPackagePathOfSystemImage("test/armabi-v7a"));
        assertEquals("system-images;android-26;test;arm64-v8a", AndroidPlatform.valueOf("android-26").getPackagePathOfSystemImage("test/arm64-v8a"));

        assertEquals("system-images;android-24;google_apis;x86", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPackagePathOfSystemImage("/x86"));
        assertEquals("system-images;android-24;google_apis;x86", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPackagePathOfSystemImage("///////x86"));

        assertEquals("system-images;android-24;google_apis;x86_64", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPackagePathOfSystemImage("x86_64/"));
        assertEquals("system-images;android-24;google_apis;x86_64", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").getPackagePathOfSystemImage("x86_64////"));

        assertEquals("system-images;android-26;google_apis;x86", AndroidPlatform.valueOf("android-26").getPackagePathOfSystemImage("google_apis/x86"));

        // check if method can handle a 'null'-ABI without NPE
        assertNotNull(AndroidPlatform.valueOf("Google Inc.:Google APIs:23").getPackagePathOfSystemImage(null));
    }

    @Test
    public void testAllPossibleVersionNames() {
        // test for plausible size
        assertEquals(AndroidPlatformVersions.values().length * 2, AndroidPlatform.getAllPossibleVersionNames().length);
        // random content check
        assertTrue(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("2.3.3"));
        assertTrue(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("7.0"));
        assertTrue(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("android-13"));
        assertTrue(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("android-26"));
        assertFalse(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("2.8.0"));
        assertFalse(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("android-0"));
        assertFalse(Arrays.asList(AndroidPlatform.getAllPossibleVersionNames()).contains("13"));
    }

    @Test
    public void testToString() {
        assertEquals("Google Inc.:Google APIs:23", AndroidPlatform.valueOf("Google Inc.:Google APIs:23").toString());
        assertEquals("Google Inc.:Google APIs:24", AndroidPlatform.valueOf("Google Inc.:Google APIs:24").toString());
        assertEquals("android-23", AndroidPlatform.valueOf("android-23").toString());
        assertEquals("android-24", AndroidPlatform.valueOf("android-24").toString());
        assertEquals("android-26", AndroidPlatform.valueOf("android-26").toString());
        assertEquals("2.3.3", AndroidPlatform.valueOf("2.3.3").toString());
        assertEquals("8.0", AndroidPlatform.valueOf("8.0").toString());
        assertEquals("26", AndroidPlatform.valueOf("26").toString());
    }
}
