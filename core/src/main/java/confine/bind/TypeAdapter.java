package confine.bind;

import confine.node.Node;

public interface TypeAdapter<T> {

    boolean supports(JavaType type);

    T read(Node node, MappingContext context);

    Node write(T value, MappingContext context);
}
