package confine.check;

import confine.internal.Scalars;
import confine.internal.Texts;

public final class MinValidator implements Validator<Object> {

    private final double minimum;
    private final String message;

    public MinValidator(double minimum, String message) {
        this.minimum = minimum;
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
        if (number < minimum) {
            return new ValidationError(path, text("value " + number + " is below minimum " + minimum));
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
