package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EmulatorOutputMatcher {

    private final List<PatternInstance> patterns = new ArrayList<PatternInstance>();
    private MatcherCallback callback = MatcherCallback.STUB;
    private String fullOutput;

    void reset() {
        fullOutput = "";
    }

    void setMatcherCallback(@Nullable MatcherCallback callback) {
        if (callback == null) {
            this.callback = MatcherCallback.STUB;
        } else {
            this.callback = callback;
        }
    }

    void registerPattern(@Nonnull Pattern pattern) {
        synchronized (patterns) {
            patterns.add(new PatternInstance(pattern));
        }
    }

    void onLineRead(@Nonnull String line) {
        synchronized (patterns) {
            fullOutput = fullOutput + line;
            for (PatternInstance patternInstance : patterns) {
                if (!patternInstance.enabled) {
                    continue;
                }
                Pattern pattern = patternInstance.pattern;
                if (isMultiLinePattern(pattern)) {
                    matchPattern(fullOutput, pattern);
                } else {
                    matchPattern(line, pattern);
                }
            }
        }
    }

    public void enablePattern(@Nonnull Pattern pattern) {
        setPatternEnabled(pattern, true);
    }

    public void disablePattern(@Nonnull Pattern pattern) {
        setPatternEnabled(pattern, false);
    }

    private void setPatternEnabled(@Nonnull Pattern pattern, boolean value) {
        synchronized (patterns) {
            for (PatternInstance patternInstance : patterns) {
                if (patternInstance.pattern == pattern) {
                    patternInstance.enabled = value;
                }
            }
        }
    }

    private boolean isMultiLinePattern(@Nonnull Pattern pattern) {
        return (pattern.flags() & Pattern.MULTILINE) != 0;
    }

    private void matchPattern(@Nonnull String content, @Nonnull Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            callback.onMatchFound(this, pattern, matcher);
        }
    }

    interface MatcherCallback {

        void onMatchFound(@Nonnull EmulatorOutputMatcher emulatorOutputMatcher, @Nonnull Pattern pattern,
                          @Nonnull Matcher matcher);

        MatcherCallback STUB = new MatcherCallback() {

            @Override
            public void onMatchFound(@Nonnull EmulatorOutputMatcher emulatorOutputMatcher, @Nonnull Pattern pattern,
                                     @Nonnull Matcher matcher) {

            }
        };
    }

    private static class PatternInstance {

        private final Pattern pattern;
        private boolean enabled;

        private PatternInstance(@Nonnull Pattern pattern) {
            this.pattern = pattern;
            this.enabled = true;
        }
    }
}
