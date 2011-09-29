package hudson.plugins.android_emulator.monkey;

import hudson.model.Action;
import hudson.plugins.android_emulator.Messages;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.kohsuke.stapler.export.Exported;

public class MonkeyAction implements Action {

    private final MonkeyResult result;
    private final int eventCount;
    private final int totalEventCount;

    public MonkeyAction(MonkeyResult outcome) {
        this(outcome, 0, 0);
    }

    public MonkeyAction(MonkeyResult outcome, int eventsCompleted, int configuredEvents) {
        this.result = outcome;
        this.eventCount = eventsCompleted;
        this.totalEventCount = configuredEvents;
    }

    @Exported
    public String getResultIcon() {
        if (result == MonkeyResult.Success) {
            return "monkey-happy_48x48.png";
        }
        return "monkey-sad_48x48.png";
    }

    @Exported
    public String getSummary() {
        String description;
        switch (result) {
        case Success:
            description = Messages.MONKEY_RESULT_SUCCESS(eventCount);
            break;
        case Crash:
            description = Messages.MONKEY_RESULT_CRASH(eventCount, totalEventCount);
            break;
        case AppNotResponding:
            description = Messages.MONKEY_RESULT_ANR(eventCount, totalEventCount);
            break;
        case UnrecognisedFormat:
            description = Messages.MONKEY_RESULT_UNRECOGNISED();
            break;
        case NothingToParse:
        default:
            description = Messages.MONKEY_RESULT_NONE();
            break;
        }
        return Messages.MONKEY_RESULT(description);
    }

    public String getDisplayName() {
        return null;
    }

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof MonkeyAction)) {
            return false;
        }

        MonkeyAction other = (MonkeyAction) that;
        return result == other.result
            && eventCount == other.eventCount
            && totalEventCount == other.totalEventCount;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(result)
            .append(eventCount)
            .append(totalEventCount)
            .toHashCode();
    }

    @Override
    public String toString() {
        return String.format("%s:%d,%d", result, eventCount, totalEventCount);
    }

}