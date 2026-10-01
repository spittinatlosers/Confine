package confine.bind;

import confine.Adapter;
import confine.BindingCache;
import confine.Comment;
import confine.Config;
import confine.Formats;
import confine.Header;
import confine.Ignore;
import confine.Key;
import confine.Nested;
import confine.Validate;
import confine.check.ConfigPredicate;
import confine.check.MaxValidator;
import confine.check.MinValidator;
import confine.check.NoPredicate;
import confine.check.NotBlankValidator;
import confine.check.NotNullValidator;
import confine.check.OneOfValidator;
import confine.check.PredicateValidator;
import confine.check.RangeValidator;
import confine.check.RegexValidator;
import confine.check.Validator;
import confine.internal.Texts;
import confine.node.Nodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BindingFactory {

    private final BindingCache cache;

    public BindingFactory(BindingCache cache) {
        if (cache == null) {
            throw new NullPointerException("cache");
        }
        this.cache = cache;
    }

    public ClassBinding binding(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        return cache.get(type, this::create);
    }

    private ClassBinding create(Class<?> type) {
        if (type.isInterface()
                || type.isPrimitive()
                || type.isArray()
                || type.isEnum()
                || Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalStateException("cannot bind " + type.getName());
        }
        Config config = type.getAnnotation(Config.class);
        String file = config == null ? "" : config.file();
        String versionKey = config == null ? "config-version" : config.versionKey();
        int version = config == null ? 1 : config.version();
        Formats format = config == null ? Formats.AUTO : config.format();
        List<String> header = readHeader(type);
        List<String> comments = readComments(type.getAnnotation(Comment.class));
        Constructor<?> constructor = classConstructor(type);
        return new ClassBinding(
                type,
                false,
                file,
                versionKey,
                version,
                format,
                header,
                comments,
                classMembers(type),
                constructor);
    }

    private List<BoundMember> classMembers(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
        Collections.reverse(hierarchy);
        Map<String, BoundMember> members = new LinkedHashMap<>();
        for (Class<?> level : hierarchy) {
            for (Field field : level.getDeclaredFields()) {
                if (skip(field)) {
                    continue;
                }
                field.setAccessible(true);
                BoundMember member = member(
                        field.getName(),
                        field.getAnnotation(Key.class),
                        JavaType.of(field.getGenericType()),
                        field.getAnnotation(Comment.class),
                        field.getAnnotation(Validate.class),
                        field.getAnnotation(Adapter.class),
                        field.getAnnotation(Nested.class) != null,
                        false,
                        field,
                        null);
                members.put(member.path(), member);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(members.values()));
    }

    private BoundMember member(
            String name,
            Key key,
            JavaType javaType,
            Comment comment,
            Validate validate,
            Adapter adapter,
            boolean nested,
            boolean ignored,
            Field field,
            Method accessor) {
        String path = key == null || Texts.blank(key.value()) ? name : key.value();
        try {
            Nodes.split(path);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("invalid key path " + path, exception);
        }
        List<Validator<Object>> validators = validators(validate, path);
        boolean required = validate != null && validate.notNull();
        return new BoundMember(
                name,
                path,
                javaType,
                readComments(comment),
                validators,
                adapter(adapter),
                required,
                nested,
                ignored,
                field,
                accessor);
    }

    private List<Validator<Object>> validators(Validate validate, String path) {
        if (validate == null) {
            return Collections.emptyList();
        }
        List<Validator<Object>> validators = new ArrayList<>();
        boolean hasMin = !Double.isNaN(validate.min());
        boolean hasMax = !Double.isNaN(validate.max());
        if (hasMin && hasMax) {
            validators.add(new RangeValidator(validate.min(), validate.max(), validate.message()));
        } else if (hasMin) {
            validators.add(new MinValidator(validate.min(), validate.message()));
        } else if (hasMax) {
            validators.add(new MaxValidator(validate.max(), validate.message()));
        }
        if (!validate.regex().isEmpty()) {
            try {
                validators.add(new RegexValidator(validate.regex(), validate.message()));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("invalid regex at " + path, exception);
            }
        }
        if (validate.notNull()) {
            validators.add(new NotNullValidator(validate.message()));
        }
        if (validate.notBlank()) {
            validators.add(new NotBlankValidator(validate.message()));
        }
        if (validate.oneOf().length > 0) {
            validators.add(new OneOfValidator(validate.oneOf(), validate.message()));
        }
        if (validate.predicate() != NoPredicate.class) {
            ConfigPredicate<?> predicate = (ConfigPredicate<?>) newInstance(validate.predicate());
            validators.add(new PredicateValidator(predicate, validate.message()));
        }
        return validators;
    }

    private TypeAdapter<?> adapter(Adapter adapter) {
        if (adapter == null) {
            return null;
        }
        return (TypeAdapter<?>) newInstance(adapter.value());
    }

    private boolean skip(Field field) {
        if (field.isSynthetic() || field.getAnnotation(Ignore.class) != null) {
            return true;
        }
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return true;
        }
        return "serialVersionUID".equals(field.getName());
    }

    private Constructor<?> classConstructor(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(type.getName() + " requires a no-argument constructor", exception);
        }
    }

    private List<String> readHeader(Class<?> type) {
        Header header = type.getAnnotation(Header.class);
        if (header == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(header.value());
    }

    private List<String> readComments(Comment comment) {
        if (comment == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(comment.value());
    }

    private Object newInstance(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("cannot create " + type.getName(), cause);
        }
    }
}
