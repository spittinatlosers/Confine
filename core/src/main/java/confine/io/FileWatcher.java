package confine.io;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FileWatcher implements AutoCloseable {

    private final long debounceMillis;
    private final WatchService watchService;
    private final Map<WatchKey, Path> directories = new ConcurrentHashMap<>();
    private final Map<Path, List<Consumer<Path>>> listeners = new ConcurrentHashMap<>();
    private final Map<Path, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounce;
    private final Thread thread;
    private volatile boolean running;

    public FileWatcher(long debounceMillis) {
        if (debounceMillis < 0) {
            throw new IllegalArgumentException("debounceMillis");
        }
        this.debounceMillis = debounceMillis;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot start file watcher", exception);
        }
        this.debounce = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread worker = new Thread(runnable);
            worker.setName("confine-watch-debounce");
            worker.setDaemon(true);
            return worker;
        });
        this.thread = new Thread(this::loop);
        this.thread.setName("confine-watch");
        this.thread.setDaemon(true);
    }

    public void watch(Path file, Consumer<Path> listener) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        Path absolute = file.toAbsolutePath().normalize();
        Path directory = absolute.getParent();
        if (directory == null) {
            throw new IllegalStateException("file has no parent: " + file);
        }
        try {
            Files.createDirectories(directory);
            listeners.computeIfAbsent(absolute, key -> new CopyOnWriteArrayList<>()).add(listener);
            if (!directories.containsValue(directory)) {
                WatchKey key = directory.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                directories.put(key, directory);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("cannot watch " + absolute, exception);
        }
        ensureStarted();
    }

    private void ensureStarted() {
        if (running) {
            return;
        }
        synchronized (this) {
            if (running) {
                return;
            }
            running = true;
            thread.start();
        }
    }

    private void loop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException exception) {
                return;
            }
            Path directory = directories.get(key);
            if (directory == null) {
                key.reset();
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    for (Path watched : Collections.unmodifiableSet(new HashSet<Path>(listeners.keySet()))) {
                        if (watched.getParent() != null && watched.getParent().equals(directory)) {
                            schedule(watched);
                        }
                    }
                    continue;
                }
                if (!(event.context() instanceof Path)) {
                    continue;
                }
                Path name = (Path) event.context();
                Path full = directory.resolve(name).toAbsolutePath().normalize();
                if (listeners.containsKey(full)) {
                    schedule(full);
                }
            }
            if (!key.reset()) {
                directories.remove(key);
            }
        }
    }

    private void schedule(Path file) {
        synchronized (pending) {
            ScheduledFuture<?> previous = pending.get(file);
            if (previous != null) {
                previous.cancel(false);
            }
            long delay = Math.max(debounceMillis, 1L);
            pending.put(file, debounce.schedule(() -> dispatch(file), delay, TimeUnit.MILLISECONDS));
        }
    }

    private void dispatch(Path file) {
        pending.remove(file);
        List<Consumer<Path>> consumers = listeners.get(file);
        if (consumers == null) {
            return;
        }
        for (Consumer<Path> consumer : consumers) {
            consumer.accept(file);
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            watchService.close();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot close file watcher", exception);
        }
        if (thread.getState() != Thread.State.NEW) {
            thread.interrupt();
            try {
                thread.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        debounce.shutdownNow();
    }
}
