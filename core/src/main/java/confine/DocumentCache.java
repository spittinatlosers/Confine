package confine;

import confine.node.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DocumentCache {

    private final long maximumSize;
    private final boolean enabled;
    private final LinkedHashMap<String, Block> values;

    public DocumentCache(long maximumSize) {
        this.maximumSize = maximumSize;
        this.enabled = maximumSize > 0L;
        if (enabled) {
            values = new LinkedHashMap<String, Block>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Block> eldest) {
                    return size() > DocumentCache.this.maximumSize;
                }
            };
        } else {
            values = null;
        }
    }

    public Block get(String key, Supplier<Block> loader) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (loader == null) {
            throw new NullPointerException("loader");
        }
        if (!enabled) {
            Block loaded = loader.get();
            if (loaded == null) {
                return null;
            }
            return loaded.copy();
        }
        synchronized (values) {
            Block cached = values.get(key);
            if (cached != null) {
                return cached.copy();
            }
            Block loaded = loader.get();
            if (loaded == null) {
                return null;
            }
            values.put(key, loaded);
            return loaded.copy();
        }
    }

    public void invalidate() {
        if (!enabled) {
            return;
        }
        synchronized (values) {
            values.clear();
        }
    }
}
