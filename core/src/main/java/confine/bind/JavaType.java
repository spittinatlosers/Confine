package confine.bind;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JavaType {

    private final Class<?> raw;
    private final List<JavaType> parameters;

    public JavaType(Class<?> raw, List<JavaType> parameters) {
        if (raw == null) {
            throw new NullPointerException("raw");
        }
        this.raw = raw;
        this.parameters = parameters == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public static JavaType of(Type type) {
        if (type == null) {
            return new JavaType(Object.class, Collections.emptyList());
        }
        if (type instanceof Class<?>) {
            Class<?> raw = (Class<?>) type;
            if (raw.isArray()) {
                return new JavaType(raw, Arrays.asList(of(raw.getComponentType())));
            }
            return new JavaType(raw, Collections.emptyList());
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Class<?> raw = (Class<?>) parameterized.getRawType();
            List<JavaType> arguments = new ArrayList<>();
            for (Type argument : parameterized.getActualTypeArguments()) {
                arguments.add(of(argument));
            }
            return new JavaType(raw, arguments);
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            JavaType component = of(arrayType.getGenericComponentType());
            Class<?> raw = Array.newInstance(component.raw(), 0).getClass();
            return new JavaType(raw, Arrays.asList(component));
        }
        if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            Type[] upper = wildcard.getUpperBounds();
            if (upper.length > 0) {
                return of(upper[0]);
            }
            return of(Object.class);
        }
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            Type[] bounds = variable.getBounds();
            if (bounds.length > 0) {
                return of(bounds[0]);
            }
            return of(Object.class);
        }
        return of(Object.class);
    }

    public Class<?> raw() {
        return raw;
    }

    public List<JavaType> parameters() {
        return parameters;
    }

    public JavaType parameter(int index) {
        if (index < 0 || index >= parameters.size()) {
            return new JavaType(Object.class, Collections.emptyList());
        }
        return parameters.get(index);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JavaType)) {
            return false;
        }
        JavaType javaType = (JavaType) other;
        return raw.equals(javaType.raw) && parameters.equals(javaType.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, parameters);
    }

    @Override
    public String toString() {
        if (parameters.isEmpty()) {
            return raw.getSimpleName();
        }
        return raw.getSimpleName() + parameters;
    }
}
