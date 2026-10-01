package confine.bind;

import confine.node.Items;
import confine.node.Node;
import confine.node.Nodes;
import confine.node.Value;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CollectionTypeAdapter implements TypeAdapter<Collection<?>> {

    @Override
    public boolean supports(JavaType type) {
        Class<?> raw = type.raw();
        return Collection.class.isAssignableFrom(raw) && !raw.isArray();
    }

    @Override
    public Collection<?> read(Node node, MappingContext context) {
        if (node == null || node instanceof Value && ((Value) node).isNull()) {
            return null;
        }
        Items list = asList(node);
        JavaType elementType = context.current().parameter(0);
        Collection<Object> values = create(context.current().raw());
        int index = 0;
        for (Node element : list.elements()) {
            context.push(String.valueOf(index));
            JavaType previous = context.current();
            context.current(elementType);
            try {
                values.add(context.registry().read(elementType, element, context, null));
            } finally {
                context.current(previous);
                context.pop();
            }
            index++;
        }
        return values;
    }

    @Override
    public Node write(Collection<?> value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        Items list = new Items();
        JavaType elementType = context.current() == null
                ? JavaType.of(Object.class)
                : context.current().parameter(0);
        int index = 0;
        for (Object element : value) {
            context.push(String.valueOf(index));
            JavaType previous = context.current();
            JavaType actual = element == null ? elementType : JavaType.of(element.getClass());
            JavaType selected = elementType.raw() == Object.class && element != null
                    ? actual
                    : elementType;
            context.current(selected);
            try {
                Node written = context.registry().write(element, selected, context, null);
                list.add(written == null ? new Value(null) : written);
            } finally {
                context.current(previous);
                context.pop();
            }
            index++;
        }
        return list;
    }

    private Items asList(Node node) {
        if (node instanceof Items) {
            Items listNode = (Items) node;
            return listNode;
        }
        if (node instanceof Value && ((Value) node).value() instanceof Collection<?>) {
            Value valueNode = (Value) node;
            Collection<?> collection = (Collection<?>) valueNode.value();
            return (Items) Nodes.fromPlain(collection);
        }
        throw new IllegalStateException("expected list at " + (node.name() == null ? "" : node.name()));
    }

    private Collection<Object> create(Class<?> raw) {
        if (raw.isInterface() || raw == List.class || raw == Collection.class) {
            return new ArrayList<>();
        }
        if (raw == Set.class) {
            return new LinkedHashSet<>();
        }
        try {
            Constructor<?> constructor = raw.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object created = constructor.newInstance();
            if (created instanceof Collection<?>) {
                Collection<?> collection = (Collection<?>) created;
                @SuppressWarnings("unchecked")
                Collection<Object> typed = (Collection<Object>) collection;
                return typed;
            }
        } catch (ReflectiveOperationException exception) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }
}
