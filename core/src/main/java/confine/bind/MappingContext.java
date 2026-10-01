package confine.bind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;

public final class MappingContext {

    private final ConfigMapper mapper;
    private final TypeAdapterRegistry registry;
    private final Deque<String> path = new ArrayDeque<>();
    private final IdentityHashMap<Object, Boolean> writing = new IdentityHashMap<>();
    private JavaType current;

    public MappingContext(ConfigMapper mapper, TypeAdapterRegistry registry) {
        if (mapper == null) {
            throw new NullPointerException("mapper");
        }
        if (registry == null) {
            throw new NullPointerException("registry");
        }
        this.mapper = mapper;
        this.registry = registry;
    }

    public ConfigMapper mapper() {
        return mapper;
    }

    public TypeAdapterRegistry registry() {
        return registry;
    }

    public JavaType current() {
        return current;
    }

    public void current(JavaType current) {
        this.current = current;
    }

    public void push(String segment) {
        if (segment != null && !segment.isEmpty()) {
            path.addLast(segment);
        }
    }

    public void pop() {
        if (!path.isEmpty()) {
            path.removeLast();
        }
    }

    public String path() {
        return String.join(".", path);
    }

    public void beginWrite(Object value) {
        if (value == null) {
            return;
        }
        if (writing.containsKey(value)) {
            throw new IllegalStateException("cyclic configuration value at " + path());
        }
        writing.put(value, Boolean.TRUE);
    }

    public void endWrite(Object value) {
        if (value == null) {
            return;
        }
        writing.remove(value);
    }
}
