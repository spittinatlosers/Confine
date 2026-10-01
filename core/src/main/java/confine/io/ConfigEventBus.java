package confine.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ConfigEventBus {

    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<>();

    public Subscription subscribe(ConfigListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void publish(ConfigEvent event) {
        if (event == null) {
            throw new NullPointerException("event");
        }
        IllegalStateException failure = null;
        for (ConfigListener listener : Collections.unmodifiableList(new ArrayList<>(listeners))) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException("config listener failed");
                }
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
