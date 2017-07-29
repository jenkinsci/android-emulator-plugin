package hudson.plugins.android_emulator.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("static-method")
public class UtilsTest extends TestCase {

    @Test
    public void testRelativeSubdirectory() {
        assertRelative("/foo/bar", "/foo/bar/baz", "baz/");
    }

    @Test
    public void testRelativeSubdirectory_TrailingSlash() {
        assertRelative("/foo/bar/", "/foo/bar/baz", "baz/");
        assertRelative("/foo/bar", "/foo/bar/baz/", "baz/");
        assertRelative("/foo/bar///", "/foo/bar/baz///", "baz/");
    }

    @Test
    public void testRelativeParentSubdirectory() {
        assertRelative("/foo/bar", "/foo/baz", "../baz/");
    }

    @Test
    public void testRelativeParentSubdirectory_TrailingSlash() {
        assertRelative("/foo/bar/", "/foo/baz", "../baz/");
        assertRelative("/foo/bar", "/foo/baz/", "../baz/");
        assertRelative("/foo/bar///", "/foo//baz//", "../baz/");
    }

    @Test
    public void testRelativeParentSubdirectory_Deeper() {
        assertRelative("/foo/bar/app", "/foo/bar/tests/unit", "../tests/unit/");
    }

    @Test
    public void testRelativePathNothingInCommon() {
        assertRelative("/a/b/c", "/d/e/f/g", "../../../d/e/f/g/");
    }

    @Test
    public void testRelativeRoot() {
        assertRelative("/", "/x", "x/");
    }

    @Test
    public void testRelativeEquivalent() {
        assertRelative("/tmp/foo", "//tmp//foo//", "");
    }

    @Test
    public void testRelativeNull() {
        assertRelative("/", null, null);
    }

    private static void assertRelative(String from, String to, String expectedResult) {
        assertEquals(expectedResult, Utils.getRelativePath(from, to));
    }

    @Test
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

    @Test
    public void testReadProperties() throws Exception {
        final File temp = File.createTempFile("temp", ".txt");
        temp.deleteOnExit();
        final PrintWriter writer = new PrintWriter(temp);
        writer.println("key=value\\\nsplit\\\nin\\\nlines\n");
        writer.close();

        final Map<String, String> map = Utils.parseConfigFile(temp);
        assertEquals(1, map.size());
    }

    @Test
    public void testVersionCompare() {
        assertTrue(Utils.isVersionOlderThan("1.0", "1.0.1"));
        assertTrue(Utils.isVersionOlderThan("1.0", "1.1"));
        assertTrue(Utils.isVersionOlderThan("1.0", "2.1"));
        assertTrue(Utils.isVersionOlderThan("1", "3.1"));

        assertTrue(Utils.isVersionOlderThan("", "1.0"));
        assertTrue(Utils.isVersionOlderThan("4.4", "4.4W"));

        assertFalse(Utils.isVersionOlderThan("2.3.3", "2.3.3"));

        assertFalse(Utils.isVersionOlderThan("2.0", "2.0.0"));
        assertFalse(Utils.isVersionOlderThan("11.1", "11.0.1"));
        assertFalse(Utils.isVersionOlderThan("4.1", "3.1"));

        assertTrue(Utils.equalsVersion("1.2.3", "1.2.3", -1));
        assertTrue(Utils.equalsVersion("1.2.3", "1.2.3", 0));
        assertTrue(Utils.equalsVersion("1.2.3", "1.2.3", 1));
        assertTrue(Utils.equalsVersion("1.2.3", "1.2.3", 2));
        assertTrue(Utils.equalsVersion("1.2.3", "1.2.3", 3));
        assertTrue(Utils.equalsVersion("1.2.3", "1.2.3", 4));

        assertTrue(Utils.equalsVersion("2.4", "2.3.4", 1));
        assertFalse(Utils.equalsVersion("2.4", "2.3.4", 2));
        assertFalse(Utils.equalsVersion("2.4", "2.3.4", 3));

        assertFalse(Utils.equalsVersion(null, "2.3.4", 3));
        assertTrue(Utils.equalsVersion(null, null, 9));
    }
}
