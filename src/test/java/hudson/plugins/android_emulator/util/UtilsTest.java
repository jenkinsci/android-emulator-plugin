package hudson.plugins.android_emulator.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

@SuppressWarnings("static-method")
public class UtilsTest extends TestCase {

    public void testParseRevisionString() throws Exception {
        assertEquals(20, Utils.parseRevisionString("20.0.1"));
        assertEquals(20, Utils.parseRevisionString("20.0"));
        assertEquals(20, Utils.parseRevisionString("20"));
        assertEquals(20, Utils.parseRevisionString("20.foo"));
        assertEquals(21, Utils.parseRevisionString("21 rc4"));
        assertEquals(21, Utils.parseRevisionString("21 rc3"));
    }

    public void testParseRevisionStringFailureCase() {
        try {
            Utils.parseRevisionString("foo");
            fail("expected exception");
        } catch (NumberFormatException e) {
            // Expected
        }
    }

    public void testRelativeSubdirectory() {
        assertRelative("/foo/bar", "/foo/bar/baz", "baz/");
    }

    public void testRelativeSubdirectory_TrailingSlash() {
        assertRelative("/foo/bar/", "/foo/bar/baz", "baz/");
        assertRelative("/foo/bar", "/foo/bar/baz/", "baz/");
        assertRelative("/foo/bar///", "/foo/bar/baz///", "baz/");
    }

    public void testRelativeParentSubdirectory() {
        assertRelative("/foo/bar", "/foo/baz", "../baz/");
    }

    public void testRelativeParentSubdirectory_TrailingSlash() {
        assertRelative("/foo/bar/", "/foo/baz", "../baz/");
        assertRelative("/foo/bar", "/foo/baz/", "../baz/");
        assertRelative("/foo/bar///", "/foo//baz//", "../baz/");
    }

    public void testRelativeParentSubdirectory_Deeper() {
        assertRelative("/foo/bar/app", "/foo/bar/tests/unit", "../tests/unit/");
    }

    public void testRelativePathNothingInCommon() {
        assertRelative("/a/b/c", "/d/e/f/g", "../../../d/e/f/g/");
    }

    public void testRelativeRoot() {
        assertRelative("/", "/x", "x/");
    }

    public void testRelativeEquivalent() {
        assertRelative("/tmp/foo", "//tmp//foo//", "");
    }

    public void testRelativeNull() {
        assertRelative("/", null, null);
    }

    private static void assertRelative(String from, String to, String expectedResult) {
        assertEquals(expectedResult, Utils.getRelativePath(from, to));
    }

    public void testRelativeDistance() {
        assertRelativeDistance("/foo/bar", "/foo/bar", 0);
        assertRelativeDistance("/foo/", "/foo/baz", 1);
        assertRelativeDistance("/foo/bar", "/foo/baz", 2);
        assertRelativeDistance("/foo/bar/app", "/foo/bar/tests/unit", 3);
        assertRelativeDistance("/", "/x", 1);
    }

    private static void assertRelativeDistance(String from, String to, int expectedResult) {
        assertEquals(expectedResult, Utils.getRelativePathDistance(from, to));
    }

    public void testGetApiLevelFromPlatform() {
        assertEquals(4, Utils.getApiLevelFromPlatform("android-4"));
        assertEquals(16, Utils.getApiLevelFromPlatform("android-16"));
        assertEquals(16, Utils.getApiLevelFromPlatform("Google Inc.:Google APIs:16"));
        assertEquals(-1, Utils.getApiLevelFromPlatform(null));
        assertEquals(-1, Utils.getApiLevelFromPlatform(""));
        assertEquals(-1, Utils.getApiLevelFromPlatform("jellybean"));
        assertEquals(-1, Utils.getApiLevelFromPlatform("Android 4.2"));
    }
    
	public void testReadProperties() throws Exception {
        final File temp = File.createTempFile("temp", ".txt");
        temp.deleteOnExit();
        final PrintWriter writer = new PrintWriter(temp);
        writer.println("key=value\\\nsplit\\\nin\\\nlines\n");
        writer.close();

        final Map<String, String> map = Utils.parseConfigFile(temp);
        assertEquals(1, map.size());
    }
}
