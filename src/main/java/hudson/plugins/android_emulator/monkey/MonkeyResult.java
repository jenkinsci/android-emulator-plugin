package hudson.plugins.android_emulator.monkey;

enum MonkeyResult {
    /** Monkey test completed successfully */
    Success,
    /** Application crashed while under test */
    Crash,
    /** ANR occurred while under test */
    AppNotResponding,
    /** No monkey output was found to parse */
    NothingToParse,
    /** Monkey output was given, but outcome couldn't be determined */
    UnrecognisedFormat
}
