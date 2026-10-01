package confine.bind;

import confine.change.VersionKeys;
import confine.internal.Scalars;
import confine.node.Block;
import confine.node.Node;
import confine.node.Nodes;
import confine.node.Value;

import java.lang.reflect.InvocationTargetException;

public final class ConfigMapper {

    private final TypeAdapterRegistry registry;
    private final BindingFactory bindings;

    public ConfigMapper(TypeAdapterRegistry registry, BindingFactory bindings) {
        if (registry == null) {
            throw new NullPointerException("registry");
        }
        if (bindings == null) {
            throw new NullPointerException("bindings");
        }
        this.registry = registry;
        this.bindings = bindings;
    }

    public TypeAdapterRegistry registry() {
        return registry;
    }

    public ClassBinding binding(Class<?> type) {
        return bindings.binding(type);
    }

    public <T> T read(Class<T> type, Block node) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (node == null) {
            throw new NullPointerException("node");
        }
        ClassBinding binding = bindings.binding(type);
        MappingContext context = new MappingContext(this, registry);
        return read(binding, node, context);
    }

    public Block write(Object value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return write(value, true, new MappingContext(this, registry));
    }

    public Object readNested(Class<?> type, Block node, MappingContext context) {
        ClassBinding binding = bindings.binding(type);
        return read(binding, node, context);
    }

    public Block writeNested(Object value, MappingContext context) {
        return write(value, false, context);
    }

    public Block schema(Class<?> type) {
        Object instance = instantiateDefaults(bindings.binding(type));
        return write(instance);
    }

    private <T> T read(ClassBinding binding, Block node, MappingContext context) {
        Object instance = newInstance(binding);
        for (BoundMember member : binding.members()) {
            if (member.ignored()) {
                continue;
            }
            Node child = Nodes.find(node, member.path());
            if (child == null) {
                continue;
            }
            context.push(member.path());
            try {
                Object value = registry.read(member.javaType(), child, context, member.adapter());
                writeField(member, instance, value);
            } finally {
                context.pop();
            }
        }
        return cast(binding.type(), instance);
    }

    private Block write(Object value, boolean root, MappingContext context) {
        ClassBinding binding = bindings.binding(value.getClass());
        context.beginWrite(value);
        try {
            Block node = new Block();
            if (root) {
                node.comments().addAll(binding.header());
                node.comments().addAll(binding.comments());
            } else {
                node.comments().addAll(binding.header());
                node.comments().addAll(binding.comments());
            }
            for (BoundMember member : binding.members()) {
                if (member.ignored()) {
                    continue;
                }
                context.push(member.path());
                try {
                    Object fieldValue = readField(member, value);
                    Node written;
                    if (fieldValue == null && member.nested()) {
                        written = new Block();
                    } else {
                        written = registry.write(
                                fieldValue,
                                member.javaType(),
                                context,
                                member.adapter());
                    }
                    if (written == null) {
                        written = new Value(null);
                    }
                    for (String comment : member.comments()) {
                        if (!written.comments().contains(comment)) {
                            written.addComment(comment);
                        }
                    }
                    Nodes.set(node, member.path(), written);
                } finally {
                    context.pop();
                }
            }
            if (root && binding.hasFile()) {
                VersionKeys.write(node, binding.versionKey(), binding.version());
            }
            return node;
        } finally {
            context.endWrite(value);
        }
    }

    private Object instantiateDefaults(ClassBinding binding) {
        return newInstance(binding);
    }

    private Object newInstance(ClassBinding binding) {
        try {
            return binding.constructor().newInstance();
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("cannot create " + binding.type().getName(), cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("cannot create " + binding.type().getName(), exception);
        }
    }

    private Object readField(BoundMember member, Object instance) {
        try {
            if (member.field() != null) {
                return member.field().get(instance);
            }
            if (member.accessor() != null) {
                return member.accessor().invoke(instance);
            }
            throw new IllegalStateException("no accessor for " + member.path());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("cannot read " + member.path(), cause);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot read " + member.path(), exception);
        }
    }

    private void writeField(BoundMember member, Object instance, Object value) {
        if (member.field() == null) {
            return;
        }
        if (value == null && member.field().getType().isPrimitive()) {
            return;
        }
        try {
            member.field().set(instance, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot write " + member.path(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Class<?> type, Object instance) {
        return (T) type.cast(instance);
    }
}
