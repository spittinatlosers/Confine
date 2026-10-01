package confine.bind;

import confine.node.Node;
import confine.node.Value;

public final class StringTypeAdapter implements TypeAdapter<String> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw() == String.class || type.raw() == CharSequence.class;
    }

    @Override
    public String read(Node node, MappingContext context) {
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            return null;
        }
        Value valueNode = (Value) node;
        return String.valueOf(valueNode.value());
    }

    @Override
    public Node write(String value, MappingContext context) {
        return new Value(value);
    }
}
