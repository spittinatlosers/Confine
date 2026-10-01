package confine.bind;

import confine.node.Node;

import java.util.ArrayList;
import java.util.List;

public final class TypeAdapterRegistry {

    private final List<TypeAdapter<?>> adapters = new ArrayList<>();

    public TypeAdapterRegistry(List<TypeAdapter<?>> custom) {
        if (custom != null) {
            for (TypeAdapter<?> adapter : custom) {
                if (adapter == null) {
                    throw new NullPointerException("adapter");
                }
                adapters.add(adapter);
            }
        }
        adapters.add(new OptionalTypeAdapter());
        adapters.add(new StringTypeAdapter());
        adapters.add(new BooleanTypeAdapter());
        adapters.add(new CharacterTypeAdapter());
        adapters.add(new NumberTypeAdapter());
        adapters.add(new EnumTypeAdapter());
        adapters.add(new TemporalTypeAdapter());
        adapters.add(new IdentifierTypeAdapter());
        adapters.add(new CollectionTypeAdapter());
        adapters.add(new MapTypeAdapter());
        adapters.add(new ArrayTypeAdapter());
        adapters.add(new AnyTypeAdapter());
        adapters.add(new ObjectTypeAdapter());
    }

    public Object read(JavaType type, Node node, MappingContext context, TypeAdapter<?> override) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        JavaType previous = context.current();
        context.current(type);
        try {
            TypeAdapter<?> adapter = override == null ? find(type) : override;
            return adapter.read(node, context);
        } finally {
            context.current(previous);
        }
    }

    public Node write(
            Object value,
            JavaType type,
            MappingContext context,
            TypeAdapter<?> override) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        JavaType previous = context.current();
        context.current(type);
        try {
            TypeAdapter<?> adapter = override == null ? find(type) : override;
            return writeWith(adapter, value, context);
        } finally {
            context.current(previous);
        }
    }

    public TypeAdapter<?> find(JavaType type) {
        for (TypeAdapter<?> adapter : adapters) {
            if (adapter.supports(type)) {
                return adapter;
            }
        }
        throw new IllegalStateException("no type adapter for " + type);
    }

    @SuppressWarnings("unchecked")
    private <T> Node writeWith(TypeAdapter<T> adapter, Object value, MappingContext context) {
        return adapter.write((T) value, context);
    }
}
