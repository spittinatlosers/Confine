package confine.bind;

import confine.node.Items;
import confine.node.Node;
import confine.node.Value;

import java.lang.reflect.Array;

public final class ArrayTypeAdapter implements TypeAdapter<Object> {

    @Override
    public boolean supports(JavaType type) {
        return type.raw().isArray();
    }

    @Override
    public Object read(Node node, MappingContext context) {
        if (node == null || node instanceof Value && ((Value) node).isNull()) {
            return null;
        }
        if (!(node instanceof Items)) {
            throw new IllegalStateException("expected array at " + context.path());
        }
        Items listNode = (Items) node;
        JavaType component = context.current().parameter(0);
        if (component.raw() == Object.class && context.current().raw().getComponentType() != null) {
            component = JavaType.of(context.current().raw().getComponentType());
        }
        Object array = Array.newInstance(component.raw(), listNode.size());
        for (int index = 0; index < listNode.size(); index++) {
            context.push(String.valueOf(index));
            JavaType previous = context.current();
            context.current(component);
            try {
                Object value = context.registry().read(
                        component,
                        listNode.get(index),
                        context,
                        null);
                Array.set(array, index, value);
            } finally {
                context.current(previous);
                context.pop();
            }
        }
        return array;
    }

    @Override
    public Node write(Object value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        int length = Array.getLength(value);
        Items list = new Items();
        Class<?> componentRaw = value.getClass().getComponentType();
        JavaType component = JavaType.of(componentRaw);
        for (int index = 0; index < length; index++) {
            context.push(String.valueOf(index));
            JavaType previous = context.current();
            context.current(component);
            try {
                Object element = Array.get(value, index);
                Node written = context.registry().write(element, component, context, null);
                list.add(written == null ? new Value(null) : written);
            } finally {
                context.current(previous);
                context.pop();
            }
        }
        return list;
    }
}
