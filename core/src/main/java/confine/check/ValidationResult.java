package confine.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {

    private final List<ValidationError> errors;

    public ValidationResult(List<ValidationError> errors) {
        if (errors == null) {
            throw new NullPointerException("errors");
        }
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static ValidationResult ok() {
        return new ValidationResult(Collections.emptyList());
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public List<ValidationError> errors() {
        return errors;
    }

    public void throwIfInvalid() {
        if (valid()) {
            return;
        }
        throw new IllegalStateException(describe());
    }

    public ValidationResult merge(ValidationResult other) {
        if (other == null || other.errors.isEmpty()) {
            return this;
        }
        if (errors.isEmpty()) {
            return other;
        }
        List<ValidationError> combined = new ArrayList<>(errors);
        combined.addAll(other.errors);
        return new ValidationResult(combined);
    }

    private String describe() {
        StringBuilder message = new StringBuilder();
        for (ValidationError error : errors) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(error.path()).append(": ").append(error.message());
        }
        return message.toString();
    }
}
