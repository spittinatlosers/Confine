package confine.check;

import confine.internal.Scalars;
import confine.internal.Texts;

public final class RangeValidator implements Validator<Object> {

    private final double minimum;
    private final double maximum;
    private final String message;

    public RangeValidator(double minimum, double maximum, String message) {
        if (maximum < minimum) {
            throw new IllegalArgumentException("maximum");
        }
        this.minimum = minimum;
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
        if (number < minimum || number > maximum) {
            return new ValidationError(path, text("value " + number + " is outside " + minimum + ".." + maximum));
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
