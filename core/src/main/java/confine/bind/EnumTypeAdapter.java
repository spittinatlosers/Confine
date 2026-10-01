package confine.bind;

import confine.node.Node;
import confine.node.Value;

public final class EnumTypeAdapter implements TypeAdapter<Enum<?>> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw().isEnum();
    }

    @Override
    public Enum<?> read(Node node, MappingContext context) {
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            return null;
        }
        Value valueNode = (Value) node;
        Class<?> raw = context.current().raw();
        String name = String.valueOf(valueNode.value()).trim();
        Object[] constants = raw.getEnumConstants();
        for (Object constant : constants) {
            Enum<?> enumerated = (Enum<?>) constant;
            if (enumerated.name().equals(name) || enumerated.name().equalsIgnoreCase(name)) {
                return enumerated;
            }
        }
        throw new IllegalStateException("unknown enum " + name + " at " + context.path());
    }

    @Override
    public Node write(Enum<?> value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        return new Value(value.name());
    }
}
