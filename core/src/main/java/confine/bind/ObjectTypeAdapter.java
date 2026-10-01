package confine.bind;

import confine.node.Block;
import confine.node.Node;
import confine.node.Value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public final class ObjectTypeAdapter implements TypeAdapter<Object> {

    @Override
    public boolean supports(JavaType type) {
        Class<?> raw = type.raw();
        if (raw.isPrimitive()
                || raw.isArray()
                || raw.isEnum()
                || raw.isInterface()
                || raw.isAnonymousClass()) {
            return false;
        }
        if (Collection.class.isAssignableFrom(raw)
                || Map.class.isAssignableFrom(raw)
                || raw == Optional.class) {
            return false;
        }
        Package location = raw.getPackage();
        if (location != null) {
            String name = location.getName();
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object read(Node node, MappingContext context) {
        if (node == null || node instanceof Value && ((Value) node).isNull()) {
            return null;
        }
        if (!(node instanceof Block)) {
            throw new IllegalStateException("expected section at " + context.path());
        }
        Block section = (Block) node;
        Class<?> raw = context.current().raw();
        return context.mapper().readNested(raw, section, context);
    }

    @Override
    public Node write(Object value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        return context.mapper().writeNested(value, context);
    }
}
