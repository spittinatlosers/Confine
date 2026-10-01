package confine.bind;

import confine.node.Block;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MapTypeAdapter implements TypeAdapter<Map<?, ?>> {

    @Override
    public boolean supports(JavaType type) {
        return Map.class.isAssignableFrom(type.raw());
    }

    @Override
    public Map<?, ?> read(Node node, MappingContext context) {
        if (node == null || node instanceof Value && ((Value) node).isNull()) {
            return null;
        }
        if (!(node instanceof Block)) {
            throw new IllegalStateException("expected object at " + context.path());
        }
        Block section = (Block) node;
        JavaType keyType = context.current().parameter(0);
        JavaType valueType = context.current().parameter(1);
        if (keyType.raw() != String.class
                && keyType.raw() != Object.class
                && !keyType.raw().isEnum()) {
            throw new IllegalStateException("map keys must be strings at " + context.path());
        }
        Map<Object, Object> values = new LinkedHashMap<>();
        for (Node child : section.ordered()) {
            if (child instanceof Note || child.name() == null) {
                continue;
            }
            Object key = key(child.name(), keyType);
            context.push(child.name());
            JavaType previous = context.current();
            context.current(valueType);
            try {
                values.put(key, context.registry().read(valueType, child, context, null));
            } finally {
                context.current(previous);
                context.pop();
            }
        }
        return values;
    }

    @Override
    public Node write(Map<?, ?> value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        Block section = new Block();
        JavaType valueType = context.current() == null
                ? JavaType.of(Object.class)
                : context.current().parameter(1);
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalStateException("null map key at " + context.path());
            }
            String key = entry.getKey() instanceof Enum<?>
                    ? ((Enum<?>) entry.getKey()).name()
                    : String.valueOf(entry.getKey());
            context.push(key);
            JavaType previous = context.current();
            context.current(valueType);
            try {
                Node written = context.registry().write(entry.getValue(), valueType, context, null);
                section.put(key, written == null ? new Value(null) : written);
            } finally {
                context.current(previous);
                context.pop();
            }
        }
        return section;
    }

    private Object key(String name, JavaType keyType) {
        if (keyType.raw().isEnum()) {
            Object[] constants = keyType.raw().getEnumConstants();
            for (Object constant : constants) {
                Enum<?> enumerated = (Enum<?>) constant;
                if (enumerated.name().equals(name)) {
                    return enumerated;
                }
            }
            throw new IllegalStateException("unknown map key " + name);
        }
        return name;
    }
}
