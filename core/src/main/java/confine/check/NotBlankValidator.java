package confine.check;

import confine.internal.Texts;

public final class NotBlankValidator implements Validator<Object> {

    private final String message;

    public NotBlankValidator(String message) {
        this.message = message == null ? "" : message;
    }

    @Override
    public ValidationError validate(String path, Object value) {
        if (!(value instanceof CharSequence)) {
            if (value == null) {
                return new ValidationError(path, text("value is blank"));
            }
            return new ValidationError(path, text("value is not text"));
        }
        CharSequence sequence = (CharSequence) value;
        if (Texts.blank(sequence.toString())) {
            return new ValidationError(path, text("value is blank"));
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
