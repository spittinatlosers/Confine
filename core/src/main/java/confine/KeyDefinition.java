package confine;

import confine.check.Validator;
import confine.internal.Texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KeyDefinition {

    private final String path;
    private Class<?> type = Object.class;
    private boolean required;
    private final List<Validator<Object>> validators = new ArrayList<>();

    public KeyDefinition(String path) {
        if (path == null || Texts.blank(path)) {
            throw new IllegalArgumentException("path");
        }
        this.path = path;
    }

    public String path() {
        return path;
    }

    public Class<?> type() {
        return type;
    }

    public void setType(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.type = type;
    }

    public boolean required() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void add(Validator<Object> validator) {
        if (validator == null) {
            throw new NullPointerException("validator");
        }
        validators.add(validator);
    }

    public List<Validator<Object>> validators() {
        return Collections.unmodifiableList(new ArrayList<>(validators));
    }
}
