package confine;

import confine.bind.ClassBinding;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class BindingCache {

    private final long maximumSize;
    private final boolean enabled;
    private final LinkedHashMap<Class<?>, ClassBinding> values;

    public BindingCache(long maximumSize) {
        this.maximumSize = maximumSize;
        this.enabled = maximumSize > 0L;
        if (enabled) {
            values = new LinkedHashMap<Class<?>, ClassBinding>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Class<?>, ClassBinding> eldest) {
                    return size() > BindingCache.this.maximumSize;
                }
            };
        } else {
            values = null;
        }
    }

    public ClassBinding get(Class<?> type, Function<Class<?>, ClassBinding> loader) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (loader == null) {
            throw new NullPointerException("loader");
        }
        if (!enabled) {
            return loader.apply(type);
        }
        synchronized (values) {
            ClassBinding found = values.get(type);
            if (found != null) {
                return found;
            }
            ClassBinding loaded = loader.apply(type);
            if (loaded != null) {
                values.put(type, loaded);
            }
            return loaded;
        }
    }
}
