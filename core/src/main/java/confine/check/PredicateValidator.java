package confine.check;

import confine.internal.Texts;

public final class PredicateValidator implements Validator<Object> {

    private final ConfigPredicate<Object> predicate;
    private final String message;

    public PredicateValidator(ConfigPredicate<?> predicate, String message) {
        if (predicate == null) {
            throw new NullPointerException("predicate");
        }
        this.predicate = cast(predicate);
        this.message = message == null ? "" : message;
    }

    @Override
    public ValidationError validate(String path, Object value) {
        boolean accepted;
        try {
            accepted = predicate.test(value);
        } catch (RuntimeException exception) {
            String text = exception.getMessage();
            if (text == null || Texts.blank(text)) {
                text = "predicate failed";
            }
            return new ValidationError(path, text);
        }
        if (accepted) {
            return null;
        }
        if (!Texts.blank(message)) {
            return new ValidationError(path, message);
        }
        String fallback = predicate.message();
        if (fallback == null || Texts.blank(fallback)) {
            fallback = "value was rejected";
        }
        return new ValidationError(path, fallback);
    }

    @SuppressWarnings("unchecked")
    private ConfigPredicate<Object> cast(ConfigPredicate<?> predicate) {
        return (ConfigPredicate<Object>) predicate;
    }
}
