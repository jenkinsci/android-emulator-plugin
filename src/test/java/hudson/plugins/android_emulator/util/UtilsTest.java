package hudson.plugins.android_emulator.util;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {
    public void testParseRevisionString() throws Exception {
        assertEquals(20, Utils.parseRevisionString("20.0.1"));
        assertEquals(20, Utils.parseRevisionString("20.0"));
        assertEquals(20, Utils.parseRevisionString("20"));
        assertEquals(20, Utils.parseRevisionString("20.foo"));
        assertEquals(21, Utils.parseRevisionString("21 rc4"));
        assertEquals(21, Utils.parseRevisionString("21 rc3"));

    }

    public void testParseRevisionStringFailureCase() throws Exception {
        try {
            Utils.parseRevisionString("foo");
            fail("expected exception");
        } catch (NumberFormatException e) {
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
        assertRelative("/a/b/c", "/tmp/foo/bar/baz", "../../../tmp/foo/bar/baz/");
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

}
