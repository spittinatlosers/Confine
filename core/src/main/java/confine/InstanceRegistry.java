package confine;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public final class InstanceRegistry {

    private final ConcurrentHashMap<Path, Class<?>> types = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Object> instances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, Long> suppressUntil = new ConcurrentHashMap<>();

    public void track(Path path, Class<?> type, Object instance) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (type == null) {
            throw new NullPointerException("type");
        }
        types.put(path, type);
        if (instance != null) {
            instances.put(type, instance);
        }
    }

    public Class<?> type(Path path) {
        return types.get(path);
    }

    public Object instance(Class<?> type) {
        return instances.get(type);
    }

    public void suppress(Path path, long nanos) {
        if (nanos <= 0L) {
            return;
        }
        suppressUntil.put(path, System.nanoTime() + nanos);
    }

    public boolean suppressed(Path path) {
        Long until = suppressUntil.get(path);
        if (until == null) {
            return false;
        }
        if (System.nanoTime() > until) {
            suppressUntil.remove(path, until);
            return false;
        }
        return true;
    }
}
