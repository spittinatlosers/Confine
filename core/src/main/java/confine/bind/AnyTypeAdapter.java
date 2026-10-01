package confine.bind;

import confine.node.Node;
import confine.node.Nodes;
import confine.node.Value;

public final class AnyTypeAdapter implements TypeAdapter<Object> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw() == Object.class && type.parameters().isEmpty();
    }

    @Override
    public Object read(Node node, MappingContext context) {
        return Nodes.plain(node);
    }

    @Override
    public Node write(Object value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        return Nodes.fromPlain(value);
    }
}
