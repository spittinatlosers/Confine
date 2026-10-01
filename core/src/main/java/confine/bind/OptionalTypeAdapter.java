package confine.bind;

import confine.node.Node;
import confine.node.Value;

import java.util.Optional;

public final class OptionalTypeAdapter implements TypeAdapter<Optional<?>> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw() == Optional.class;
    }

    @Override
    public Optional<?> read(Node node, MappingContext context) {
        if (node == null || node instanceof Value && ((Value) node).isNull()) {
            return Optional.empty();
        }
        JavaType inner = context.current().parameter(0);
        JavaType previous = context.current();
        context.current(inner);
        try {
            Object value = context.registry().read(inner, node, context, null);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(value);
        } finally {
            context.current(previous);
        }
    }

    @Override
    public Node write(Optional<?> value, MappingContext context) {
        if (value == null || !value.isPresent()) {
            return new Value(null);
        }
        JavaType inner = context.current() == null
                ? JavaType.of(Object.class)
                : context.current().parameter(0);
        Object nested = value.get();
        JavaType selected = inner.raw() == Object.class ? JavaType.of(nested.getClass()) : inner;
        JavaType previous = context.current();
        context.current(selected);
        try {
            return context.registry().write(nested, selected, context, null);
        } finally {
            context.current(previous);
        }
    }
}
