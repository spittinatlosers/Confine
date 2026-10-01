package confine.check;

import confine.internal.Texts;

import java.util.regex.Pattern;

public final class RegexValidator implements Validator<Object> {

    private final Pattern pattern;
    private final String message;

    public RegexValidator(String regex, String message) {
        if (regex == null || regex.isEmpty()) {
            throw new IllegalArgumentException("regex");
        }
        this.pattern = Pattern.compile(regex);
        this.message = message == null ? "" : message;
    }

    @Override
    public ValidationError validate(String path, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof CharSequence)) {
            return new ValidationError(path, text("value is not text"));
        }
        CharSequence sequence = (CharSequence) value;
        if (!pattern.matcher(sequence).matches()) {
            return new ValidationError(path, text("value does not match " + pattern.pattern()));
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
