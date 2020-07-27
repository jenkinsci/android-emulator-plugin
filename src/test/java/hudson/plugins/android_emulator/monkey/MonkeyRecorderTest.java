package hudson.plugins.android_emulator.monkey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;

public class MonkeyRecorderTest extends TestCase {

    private static final String MONKEY_CRASH = "// CRASH";
    private static final String MONKEY_ANR = "// NOT RESPONDING";
    private static final String MONKEY_SUCCESS = "// Monkey finished";

    public void testNotRunForBuild_Aborted() throws InterruptedException, IOException {
        assertNoParsingForBadBuild(Result.ABORTED);
    }

    public void testNotRunForBuild_Failure() throws InterruptedException, IOException {
        assertNoParsingForBadBuild(Result.FAILURE);
    }

    public void testNotRunForBuild_NotBuild() throws InterruptedException, IOException {
        assertNoParsingForBadBuild(Result.NOT_BUILT);
    }

    private void assertNoParsingForBadBuild(Result buildResult) throws InterruptedException, IOException {
        // Set up build with result
        AbstractBuild<?,?> build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(buildResult);

        // Execute monkey recorder which would alter the build result if it runs
        MonkeyRecorder recorder = new MonkeyRecorder(null, BuildOutcome.UNSTABLE);
        boolean result = recorder.perform(build, mock(Launcher.class), mock(BuildListener.class));
        assertTrue(result);

        // Ensure the build result is not updated
        verify(build, never()).setResult(any(Result.class));
    }

    public void testNullInput() {
        parseOutputAndAssert(null, MonkeyResult.NothingToParse);
    }

    public void testEmptyInput() {
        parseOutputAndAssert("", MonkeyResult.UnrecognisedFormat);
    }

    public void testTruncatedInput() {
        parseOutputAndAssert("// CRAS", MonkeyResult.UnrecognisedFormat);
    }

    public void testCrash() {
        parseOutputAndAssert(MONKEY_CRASH, MonkeyResult.Crash);
    }

    public void testCrash_FailureBegetsFailure() {
        parseOutputAndAssert(MONKEY_CRASH, BuildOutcome.FAILURE, Result.FAILURE, MonkeyResult.Crash);
    }

    public void testCrash_UnstableBegetsUnstable() {
        parseOutputAndAssert(MONKEY_CRASH, BuildOutcome.UNSTABLE, Result.UNSTABLE, MonkeyResult.Crash);
    }

    public void testAppNotResponding() {
        parseOutputAndAssert(MONKEY_ANR, MonkeyResult.AppNotResponding);
    }

    public void testAppNotResponding_FailureBegetsFailure() {
        parseOutputAndAssert(MONKEY_ANR, BuildOutcome.FAILURE, Result.FAILURE, MonkeyResult.AppNotResponding);
    }

    public void testAppNotResponding_UnstableBegetsUnstable() {
        parseOutputAndAssert(MONKEY_ANR, BuildOutcome.UNSTABLE, Result.UNSTABLE, MonkeyResult.AppNotResponding);
    }

    public void testSuccess() {
        parseOutputAndAssert(MONKEY_SUCCESS, MonkeyResult.Success);
    }

    public void testTotalEventCount() {
        String output = ":Monkey: seed=0 count=1234";
        parseOutputAndAssert(output, MonkeyResult.UnrecognisedFormat, 0, 1234);
    }

    public void testTotalEventCountWithActualCount() {
        String output = ":Monkey: seed=0 count=1234\n";
        output += "Events injected: 999";
        parseOutputAndAssert(output, MonkeyResult.UnrecognisedFormat, 999, 1234);
    }

    public void testSuccessWithTotalCount() {
        String output = ":Monkey: seed=0 count=1234\n";
        output += MONKEY_SUCCESS;
        parseOutputAndAssert(output, MonkeyResult.Success, 1234, 1234);
    }

    private void parseOutputAndAssert(String monkeyOutput, MonkeyResult expectedResult) {
        parseOutputAndAssert(monkeyOutput, BuildOutcome.IGNORE, null, expectedResult, 0, 0);
    }

    private void parseOutputAndAssert(String monkeyOutput, MonkeyResult expectedResult,
            int expectedEvents, int expectedTotalEvents) {
        parseOutputAndAssert(monkeyOutput, BuildOutcome.IGNORE, null, expectedResult,
                expectedEvents, expectedTotalEvents);
    }

    private void parseOutputAndAssert(String monkeyOutput, BuildOutcome desiredOutcome,
            Result expectedBuildResult, MonkeyResult expectedResult) {
        parseOutputAndAssert(monkeyOutput, desiredOutcome, expectedBuildResult, expectedResult, 0, 0);
    }

    private void parseOutputAndAssert(String monkeyOutput, BuildOutcome desiredOutcome,
            Result expectedBuildResult, MonkeyResult expectedResult, int expectedEvents,
            int expectedTotalEvents) {
        AbstractBuild<?,?> build = mock(AbstractBuild.class);
        PrintStream logger = mock(PrintStream.class);

        // Parse the given output
        MonkeyAction action = MonkeyRecorder.parseMonkeyOutput(build, logger, monkeyOutput, desiredOutcome);

        // Assert that the build result has been updated (or not)
        if (desiredOutcome == BuildOutcome.IGNORE) {
            verify(build, never()).setResult(any(Result.class));
        } else {
            verify(build).setResult(expectedBuildResult);
        }

        // An Action should have been returned; assert it has the expected properties
        assertNotNull(action);
        MonkeyAction expected = new MonkeyAction(expectedResult, expectedEvents, expectedTotalEvents);
        assertEquals(expected, action);
    }

}