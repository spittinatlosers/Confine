package confine.bind;

import confine.check.Validator;
import confine.internal.Texts;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BoundMember {

    private final String name;
    private final String path;
    private final JavaType javaType;
    private final List<String> comments;
    private final List<Validator<Object>> validators;
    private final TypeAdapter<?> adapter;
    private final boolean required;
    private final boolean nested;
    private final boolean ignored;
    private final Field field;
    private final Method accessor;

    public BoundMember(
            String name,
            String path,
            JavaType javaType,
            List<String> comments,
            List<Validator<Object>> validators,
            TypeAdapter<?> adapter,
            boolean required,
            boolean nested,
            boolean ignored,
            Field field,
            Method accessor) {
        if (name == null || Texts.blank(name)) {
            throw new IllegalArgumentException("name");
        }
        if (path == null || Texts.blank(path)) {
            throw new IllegalArgumentException("path");
        }
        if (javaType == null) {
            throw new NullPointerException("javaType");
        }
        this.name = name;
        this.path = path;
        this.javaType = javaType;
        this.comments = comments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(comments));
        this.validators = validators == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(validators));
        this.adapter = adapter;
        this.required = required;
        this.nested = nested;
        this.ignored = ignored;
        this.field = field;
        this.accessor = accessor;
    }

    public String name() {
        return name;
    }

    public String path() {
        return path;
    }

    public JavaType javaType() {
        return javaType;
    }

    public List<String> comments() {
        return comments;
    }

    public List<Validator<Object>> validators() {
        return validators;
    }

    public TypeAdapter<?> adapter() {
        return adapter;
    }

    public boolean required() {
        return required;
    }

    public boolean nested() {
        return nested;
    }

    public boolean ignored() {
        return ignored;
    }

    public Field field() {
        return field;
    }

    public Method accessor() {
        return accessor;
    }
}
