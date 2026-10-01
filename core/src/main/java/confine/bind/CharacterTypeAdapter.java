package confine.bind;

import confine.internal.Scalars;
import confine.node.Node;
import confine.node.Value;

public final class CharacterTypeAdapter implements TypeAdapter<Character> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw() == char.class || type.raw() == Character.class;
    }

    @Override
    public Character read(Node node, MappingContext context) {
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            if (context.current() != null && context.current().raw() == char.class) {
                return '\0';
            }
            return null;
        }
        Value valueNode = (Value) node;
        Character parsed = Scalars.tryChar(valueNode.value());
        if (parsed == null) {
            throw new IllegalStateException("expected character at " + context.path());
        }
        return parsed;
    }

    @Override
    public Node write(Character value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        return new Value(String.valueOf(value));
    }
}
