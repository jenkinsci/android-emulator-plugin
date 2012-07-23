package hudson.plugins.android_emulator.util;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {
    public void testParseRevisionString() throws Exception {
        assertEquals(20, Utils.parseRevisionString("20.0.1"));
        assertEquals(20, Utils.parseRevisionString("20.0"));
        assertEquals(20, Utils.parseRevisionString("20"));
    }

    public void testParseRevisionStringFailureCase() throws Exception {
        try {
            Utils.parseRevisionString("foo");
            fail("expected exception");
        } catch (NumberFormatException e) {
        }

        try {
            Utils.parseRevisionString("20.foo");
            fail("expected exception");
        } catch (NumberFormatException e) {
        }
    }
}
