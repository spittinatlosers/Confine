package confine.bind;

import confine.node.Node;
import confine.node.Value;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public final class IdentifierTypeAdapter implements TypeAdapter<Object> {

    @Override
    public boolean supports(JavaType type) {
        Class<?> raw = type.raw();
        return raw == UUID.class || raw == Path.class || raw == URI.class || raw == URL.class;
    }

    @Override
    public Object read(Node node, MappingContext context) {
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            return null;
        }
        Value valueNode = (Value) node;
        Class<?> raw = context.current().raw();
        String text = String.valueOf(valueNode.value()).trim();
        try {
            if (raw == UUID.class) {
                return UUID.fromString(text);
            }
            if (raw == Path.class) {
                return Paths.get(text);
            }
            if (raw == URI.class) {
                return URI.create(text);
            }
            if (raw == URL.class) {
                return URI.create(text).toURL();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("expected "
                    + raw.getSimpleName()
                    + " at "
                    + context.path(), exception);
        }
        throw new IllegalStateException("unsupported identifier at " + context.path());
    }

    @Override
    public Node write(Object value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        return new Value(value.toString());
    }
}
