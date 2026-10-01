package confine.check;

import confine.internal.Texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OneOfValidator implements Validator<Object> {

    private final List<String> allowed;
    private final String message;

    public OneOfValidator(String[] allowed, String message) {
        if (allowed == null || allowed.length == 0) {
            throw new IllegalArgumentException("allowed");
        }
        List<String> values = new ArrayList<>();
        for (String value : allowed) {
            if (value == null) {
                throw new NullPointerException("allowed");
            }
            values.add(value);
        }
        this.allowed = Collections.unmodifiableList(new ArrayList<>(values));
        this.message = message == null ? "" : message;
    }

    @Override
    public ValidationError validate(String path, Object value) {
        if (value == null) {
            return null;
        }
        String text = value instanceof Enum<?> ? ((Enum<?>) value).name() : String.valueOf(value);
        for (String candidate : allowed) {
            if (candidate.equals(text)) {
                return null;
            }
        }
        if (Texts.blank(message)) {
            return new ValidationError(path, "value must be one of " + allowed);
        }
        return new ValidationError(path, message);
    }
}
