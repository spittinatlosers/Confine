package confine.io;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class AsyncConfigIo implements AutoCloseable {

    private final ExecutorService executor;

    public AsyncConfigIo(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("threads");
        }
        this.executor = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("confine-io");
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        if (supplier == null) {
            throw new NullPointerException("supplier");
        }
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> run(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }
        return CompletableFuture.runAsync(runnable, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("io shutdown interrupted", exception);
        }
    }
}
