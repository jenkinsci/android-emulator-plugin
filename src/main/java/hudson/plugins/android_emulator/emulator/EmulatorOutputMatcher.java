package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EmulatorOutputMatcher {

    private final List<Pattern> patterns = new ArrayList<Pattern>();
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
            patterns.add(pattern);
        }
    }

    void onLineRead(@Nonnull String line) {
        synchronized (patterns) {
            fullOutput = fullOutput + line;
            for (Pattern pattern : patterns) {
                matchPattern(fullOutput, pattern);
            }
        }
    }

    private void matchPattern(@Nonnull String content, @Nonnull Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            callback.onMatchFound(pattern, matcher);
        }
    }

    interface MatcherCallback {

        void onMatchFound(@Nonnull Pattern pattern, @Nonnull Matcher matcher);

        MatcherCallback STUB = new MatcherCallback() {

            @Override
            public void onMatchFound(@Nonnull Pattern pattern, @Nonnull Matcher matcher) {

            }
        };
    }
}
