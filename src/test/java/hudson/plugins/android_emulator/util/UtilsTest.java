package hudson.plugins.android_emulator.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jvnet.hudson.test.Issue;

import hudson.EnvVars;
import org.junit.jupiter.api.Test;

class UtilsTest {

    private static final String ENV_VAR_NAME_QEMU_AUDIO_DRIVER = "QEMU_AUDIO_DRV";
    private static final String ENV_VAR_VALUE_QEMU_AUDIO_DRIVER_NONE = "none";

    @Test
    void testRelativeSubdirectory() {
        assertRelative("/foo/bar", "/foo/bar/baz", "baz/");
    }

    @Test
    void testRelativeSubdirectory_TrailingSlash() {
        assertRelative("/foo/bar/", "/foo/bar/baz", "baz/");
        assertRelative("/foo/bar", "/foo/bar/baz/", "baz/");
        assertRelative("/foo/bar///", "/foo/bar/baz///", "baz/");
    }

    @Test
    void testRelativeParentSubdirectory() {
        assertRelative("/foo/bar", "/foo/baz", "../baz/");
    }

    @Test
    void testRelativeParentSubdirectory_TrailingSlash() {
        assertRelative("/foo/bar/", "/foo/baz", "../baz/");
        assertRelative("/foo/bar", "/foo/baz/", "../baz/");
        assertRelative("/foo/bar///", "/foo//baz//", "../baz/");
    }

    @Test
    void testRelativeParentSubdirectory_Deeper() {
        assertRelative("/foo/bar/app", "/foo/bar/tests/unit", "../tests/unit/");
    }

    @Test
    void testRelativePathNothingInCommon() {
        assertRelative("/a/b/c", "/d/e/f/g", "../../../d/e/f/g/");
    }

    @Test
    void testRelativeRoot() {
        assertRelative("/", "/x", "x/");
    }

    @Test
    void testRelativeEquivalent() {
        assertRelative("/tmp/foo", "//tmp//foo//", "");
    }

    @Test
    void testRelativeNull() {
        assertRelative("/", null, null);
        assertRelative(null, "/", null);
    }

    private static void assertRelative(String from, String to, String expectedResult) {
        assertEquals(FilenameUtils.separatorsToSystem(expectedResult), Utils.getRelativePath(from, to));
    }

    @Test
    void testNormalizePathSeparator() {
        assertEquals("/a/b/c", Utils.normalizePathSeparators("//a/////b//c"));
        assertEquals("/a/../b/c", Utils.normalizePathSeparators("//a//..///b//c"));
        assertEquals("\\\\HOST\\b\\c\\", Utils.normalizePathSeparators("\\\\\\HOST\\\\\\\\\\b\\\\\\c\\\\"));
    }

    @Test
    void testRelativeDistance() {
        assertRelativeDistance("/foo/bar", "/foo/bar", 0);
        assertRelativeDistance("/foo/", "/foo/baz", 1);
        assertRelativeDistance("/foo/bar", "/foo/baz", 2);
        assertRelativeDistance("/foo/bar/app", "/foo/bar/tests/unit", 3);
        assertRelativeDistance("/", "/x", 1);
        assertRelativeDistance("/", null, -1);
        assertRelativeDistance("\\\\HOST\\a\\b", "\\\\HOST\\a\\c", 2);
    }

    private static void assertRelativeDistance(String from, String to, int expectedResult) {
        assertEquals(expectedResult, Utils.getRelativePathDistance(from, to));
    }

    @Test
    void testReadUnsupportedConfigFile() throws Exception {
        final File temp = Files.createTempFile("temp", ".txt").toFile();
        temp.deleteOnExit();

        try {
            ConfigFileUtils.parseConfigFile(temp);
            fail("Expected exception");
        } catch (IOException expectedException) {
            // expected path
        } catch (Exception unexpectedException) {
            fail("Unexpected exception thrown: " + unexpectedException.getClass().getName());
        }
    }

    @Test
    void testReadConfigFileInPropertiesFormat() throws Exception {
        final File temp = Files.createTempFile("temp", ".properties").toFile();
        temp.deleteOnExit();

        // test multiline props
        writeContentToTestFile(temp, "key=value\\\nsplit\\\nin\\\nlines\n");

        assertEquals(1, ConfigFileUtils.parseConfigFile(temp).size());

        // test property behavior  to keep backslash non line break indicator
        writeContentToTestFile(temp, "key=slashes\\got\\deleted\\here\n");

        assertEquals("slashesgotdeletedhere",
                ConfigFileUtils.parseConfigFile(temp).get("key"));
    }

    @Test
    void testReadConfigFileInINIFormat() throws Exception {
        final File temp = Files.createTempFile("temp", ".ini").toFile();
        temp.deleteOnExit();

        // value should be returned 'as-is' without removal of '\'
        writeContentToTestFile(temp, """
				key=system-images\\android-24\\default\\x86_64
				xxx

				test=test\\
				test2=test2
				#comment=1
				;comment2=2
				=nokey
				novalue=
				=""");

        assertEquals(5, ConfigFileUtils.parseConfigFile(temp).size());

        assertEquals("system-images\\android-24\\default\\x86_64",
                ConfigFileUtils.parseConfigFile(temp).get("key"));
        assertEquals("",
                ConfigFileUtils.parseConfigFile(temp).get("xxx"));
        assertEquals("test\\",
                ConfigFileUtils.parseConfigFile(temp).get("test"));
        assertEquals("test2",
                ConfigFileUtils.parseConfigFile(temp).get("test2"));
        assertEquals("",
                ConfigFileUtils.parseConfigFile(temp).get("novalue"));
    }

    @Test
    void testWriteUnsupportedConfigFile() throws Exception {
        final File temp = Files.createTempFile("temp", ".txt").toFile();
        temp.deleteOnExit();

        try {
            ConfigFileUtils.writeConfigFile(temp, new HashMap<>());
            fail("Expected exception");
        } catch (IOException expectedException) {
            // expected path
        } catch (Exception unexpectedException) {
            fail("Unexpected exception thrown: " + unexpectedException.getClass().getName());
        }
    }

    @Test
    void testWriteConfigFileInPropertiesFormat() throws Exception {
        final File expected = Files.createTempFile("temp", ".properties").toFile();
        expected.deleteOnExit();

        final File actual = Files.createTempFile("temp", ".properties").toFile();
        actual.deleteOnExit();

        final String newLine = (SystemUtils.IS_OS_WINDOWS) ? "\r\n" : "\n";

        // test pair-wise, as we do not know the properties order

        // Test 1
        // Setup test data
        final Map<String, String> keyValues = new HashMap<>();
        keyValues.put("key","some\\back\\slash\\value");

        ConfigFileUtils.writeConfigFile(actual, keyValues);

        // write expected data
        String timestamp = readFirstLineOfFile(actual);
        writeContentToTestFile(expected, timestamp + newLine +
                "key=some\\\\back\\\\slash\\\\value" + newLine);

        assertTrue(FileUtils.contentEquals(expected, actual), "File contents differ!");

        // Test 2
        // Setup test data
        keyValues.clear();
        keyValues.put("xxx","");

        ConfigFileUtils.writeConfigFile(actual, keyValues);

        // write expected data
        timestamp = readFirstLineOfFile(actual);
        writeContentToTestFile(expected, timestamp + newLine +
                "xxx=" + newLine);

        assertTrue(FileUtils.contentEquals(expected, actual), "File contents differ!");
    }

    @Test
    void testWriteConfigFileInINIFormat() throws Exception {
        final File expected = Files.createTempFile("temp", ".ini").toFile();
        expected.deleteOnExit();

        final File actual = Files.createTempFile("temp", ".ini").toFile();
        actual.deleteOnExit();

        // Setup test data
        final Map<String, String> keyValues = new LinkedHashMap<>();
        keyValues.put("key","system-images\\android-24\\default\\x86_64");
        keyValues.put("xxx", "");
        keyValues.put("test", "test\\");
        keyValues.put("test2", "test2");
        ConfigFileUtils.writeConfigFile(actual, keyValues);

        // write expected data
        writeContentToTestFile(expected, "key=system-images\\android-24\\default\\x86_64\r\nxxx=\r\ntest=test\\\r\ntest2=test2\r\n");

        assertTrue(FileUtils.contentEquals(expected, actual));
    }

    private void writeContentToTestFile(final File file, final String content) throws Exception {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(content);
        }
    }

    private String readFirstLineOfFile(final File file) throws Exception {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            return bufferedReader.readLine();
        }
    }

    @Test
    void testVersionCompare() {
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

    @Test
    void testfindPatternWithHighestVersionSuffix() {
        final String versions1 = "build-tools-3.3\nbuild-tools-0.1\rbuild-tools-1\r\nbuild-tools-3.1";
        final String result1 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions1, "build-tools");
        assertEquals("build-tools-3.3", result1);


        final String versions2 = "build-tools;3.3\nbuild-tools;0.1\rbuild-tools;4\r\nbuild-tools;3.1";
        final String result2 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions2, "build-tools");
        assertEquals("build-tools;4", result2);

        final String versions3 = "    build-tools;3.3     \n \"build-tools;0.1\"\rbuild-tools;4\r\nbuild-tools;3.1";
        final String result3 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions3, "build-tools");
        assertEquals("build-tools;4", result3);

        final String versions4 = "    build-tools;15.3     \n \"build-tools;0.1\"\rbuild-tools;4\r\nbuild-tools;3.1";
        final String result4 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions4, "build-tools");
        assertEquals("build-tools;15.3", result4);

        final String versions5 = "    build-tools;5.3     \n \"build-tools;6.1\"\rbuild-tools;4\r\nbuild-tools;3.1";
        final String result5 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions5, "build-tools");
        assertEquals("build-tools;6.1", result5);

        final String version6 = "id: 3 or \"build-tools-26.0.1\"";
        final String result6 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(version6, "build-tools");
        assertEquals("build-tools-26.0.1", result6);

        final String versions7 = "build-tools-3.3\nbild-tools-20.1\rbuild-tools-1\r\nbuild-tools-3.1";
        final String result7 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions7, "build-tools");
        assertEquals("build-tools-3.3", result7);

        final String versions8 = "build-tools-3.3\nbild-tools-20.1\rbuild-tools-1\r\nbuild-tools-3.1";
        final String result8 = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(versions8, "dummy-tools");
        assertNull(result8);
    }

    @Issue("JENKINS-51197")
    @Test
    void testPatternExcludeSnapshotVersion() {
        String input = "build-tools-27.0.0\nbuild-tools-27.0.1\rbuild-tools-30.0.0-rc2";
        String result = Utils.getPatternWithHighestSuffixedVersionNumberInMultiLineInput(input, "build-tools");
        assertEquals("build-tools-27.0.1", result);
    }

    // Workaround for Bug 64356053 ('-no-audio'-, '-noaudio'- and '-audio none'-options ignored)
    @Test
    void testQemu2AudioEnvSettingFromCommandLine() {
        assertEnvironmentDisablesAudioDriver(Utils.getEnvironmentVarsFromEmulatorArgs("--test -no-audio --xx"));
        assertEnvironmentDisablesAudioDriver(Utils.getEnvironmentVarsFromEmulatorArgs("-noaudio --xx"));
        assertEnvironmentDisablesAudioDriver(Utils.getEnvironmentVarsFromEmulatorArgs("something -noaudio"));
        assertEnvironmentDisablesAudioDriver(Utils.getEnvironmentVarsFromEmulatorArgs("-audio none"));

        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs("--no-audio --xx"));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs("--test --noaudio"));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs("-test -audio something"));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs("--audio"));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs("-audio"));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs(""));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs("-something"));
        assertEnvironmentNoAudioDriverSetting(Utils.getEnvironmentVarsFromEmulatorArgs(null));
    }

    /**
     * Checks if given {@code EnvVars} contain 'QEMU_AUDIO_DRV=none' which would disable audio
     *
     * @param envDisableAudio environment variables to check if they contain 'QEMU_AUDIO_DRV=none'
     */
    private void assertEnvironmentDisablesAudioDriver(final EnvVars envDisableAudio) {
        assertTrue(envDisableAudio.containsKey(ENV_VAR_NAME_QEMU_AUDIO_DRIVER));
        assertEquals(ENV_VAR_VALUE_QEMU_AUDIO_DRIVER_NONE, envDisableAudio.get(ENV_VAR_NAME_QEMU_AUDIO_DRIVER));
    }

    /**
     * Checks if given {@code EnvVars} does not contain the 'QEMU_AUDIO_DRV' option, so audio would not
     * be influenced by environment
     *
     * @param envDisableAudio environment variables to check if they does not contain 'QEMU_AUDIO_DRV'
     */
    private void assertEnvironmentNoAudioDriverSetting(final EnvVars envDisableAudio) {
        assertFalse(envDisableAudio.containsKey(ENV_VAR_NAME_QEMU_AUDIO_DRIVER));
        assertNull(envDisableAudio.get(ENV_VAR_NAME_QEMU_AUDIO_DRIVER));
    }
}
