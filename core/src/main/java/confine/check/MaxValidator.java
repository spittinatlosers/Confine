package confine.check;

import confine.internal.Scalars;
import confine.internal.Texts;

public final class MaxValidator implements Validator<Object> {

    private final double maximum;
    private final String message;

    public MaxValidator(double maximum, String message) {
        this.maximum = maximum;
        this.message = message == null ? "" : message;
    }

    @Override
    public ValidationError validate(String path, Object value) {
        if (value == null) {
            return null;
        }
        Double number = Scalars.tryDouble(value);
        if (number == null) {
            return new ValidationError(path, text("value is not numeric"));
        }
        if (number > maximum) {
            return new ValidationError(path, text("value " + number + " is above maximum " + maximum));
        }
        return null;
    }

    private String text(String fallback) {
        if (Texts.blank(message)) {
            return fallback;
        }
        return message;
    }
}
