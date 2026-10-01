package confine.bind;

import confine.internal.Scalars;
import confine.node.Node;
import confine.node.Value;

public final class BooleanTypeAdapter implements TypeAdapter<Boolean> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw() == boolean.class || type.raw() == Boolean.class;
    }

    @Override
    public Boolean read(Node node, MappingContext context) {
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            if (context.current() != null && context.current().raw() == boolean.class) {
                return false;
            }
            return null;
        }
        Value valueNode = (Value) node;
        Boolean parsed = Scalars.tryBoolean(valueNode.value());
        if (parsed == null) {
            throw new IllegalStateException("expected boolean at " + context.path());
        }
        return parsed;
    }

    @Override
    public Node write(Boolean value, MappingContext context) {
        return new Value(value);
    }
}
