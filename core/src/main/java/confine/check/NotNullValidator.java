package confine.check;

import confine.internal.Texts;

public final class NotNullValidator implements Validator<Object> {

    private final String message;

    public NotNullValidator(String message) {
        this.message = message == null ? "" : message;
    }

    @Override
    public ValidationError validate(String path, Object value) {
        if (value != null) {
            return null;
        }
        if (Texts.blank(message)) {
            return new ValidationError(path, "value is required");
        }
        return new ValidationError(path, message);
    }
}
