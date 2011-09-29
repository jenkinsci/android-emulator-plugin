package hudson.plugins.android_emulator.util;

import hudson.util.FormValidation;

public class ValidationResult {

    public static enum Type {
        OK,
        WARNING,
        ERROR
    }

    private final Type type;
    private final String message;
    private final boolean hasMarkup;

    public ValidationResult(Type type, String message) {
        this(type, message, false);
    }

    public ValidationResult(Type type, String message, boolean hasMarkup) {
        this.type = type;
        this.message = message;
        this.hasMarkup = hasMarkup;
    }

    public static ValidationResult ok() {
        return new ValidationResult(Type.OK, null);
    }

    public static ValidationResult warning(String message) {
        return new ValidationResult(Type.WARNING, message);
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(Type.ERROR, message);
    }

    public static ValidationResult errorWithMarkup(String message) {
        return new ValidationResult(Type.ERROR, message, true);
    }

    public FormValidation getFormValidation() {
        switch (type) {
        case WARNING:
            return FormValidation.warning(message);
        case ERROR:
            if (hasMarkup) {
                return FormValidation.errorWithMarkup(message);
            } else {
                return FormValidation.error(message);
            }
        }

        return FormValidation.ok();
    }

    public boolean isFatal() {
        return type == Type.ERROR;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidationResult[type="+ type +", message="+ message +"]";
    }

}
