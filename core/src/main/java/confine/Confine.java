package confine;

import confine.bind.BindingFactory;
import confine.bind.ConfigMapper;
import confine.bind.SchemaGenerator;
import confine.bind.TypeAdapterRegistry;
import confine.change.AnnotationMigrationSource;
import confine.change.BackupService;
import confine.change.MigrationBuilder;
import confine.change.MigrationEngine;
import confine.change.MigrationRegistry;
import confine.check.DefaultMerger;
import confine.check.ValidationEngine;
import confine.internal.Texts;
import confine.io.AsyncConfigIo;
import confine.io.ConfigEvent;
import confine.io.ConfigEventBus;
import confine.io.ConfigListener;
import confine.io.ConfigStore;
import confine.io.FileWatcher;
import confine.io.ResourceReader;
import confine.io.Subscription;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Confine implements AutoCloseable {

    private final Settings config;
    private final Registry formats;
    private final ConfigMapper mapper;
    private final DocumentPipeline pipeline;
    private final MigrationRegistry migrations;
    private final ConfigEventBus events;
    private final FileWatcher watcher;
    private final AsyncConfigIo async;
    private final InstanceRegistry instances;
    private final DocumentCache documents;
    private final Set<Path> watched = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    Confine(Settings config) {
        if (config == null) {
            throw new NullPointerException("config");
        }
        this.config = config;
        this.formats = new Registry();
        this.events = new ConfigEventBus();
        this.instances = new InstanceRegistry();
        this.migrations = new MigrationRegistry();
        this.documents = new DocumentCache(config.cacheMaximumSize());
        TypeAdapterRegistry adapters = new TypeAdapterRegistry(config.adapters());
        BindingFactory bindings = new BindingFactory(new BindingCache(config.cacheMaximumSize()));
        this.mapper = new ConfigMapper(adapters, bindings);
        SchemaGenerator schemas = new SchemaGenerator(mapper);
        this.async = new AsyncConfigIo(config.ioThreads());
        this.watcher = new FileWatcher(config.watchDebounceMillis());
        this.pipeline = new DocumentPipeline(
                config,
                formats,
                mapper,
                schemas,
                new MigrationEngine(),
                migrations,
                new AnnotationMigrationSource(),
                new ValidationEngine(),
                new DefaultMerger(),
                new ConfigStore(),
                new ResourceReader(config.classLoader(), config.charset()),
                new BackupService(),
                documents,
                events,
                instances);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SchemaBuilder schema() {
        return new SchemaBuilder();
    }

    public Settings config() {
        return config;
    }

    public Registry formats() {
        return formats;
    }

    public SchemaBuilder document() {
        return new SchemaBuilder();
    }

    public <T> T load(Class<T> type) {
        ensureOpen();
        T instance = pipeline.load(type, null, LoadOrigin.USER);
        watchLoaded(type, null);
        return instance;
    }

    public <T> T load(Class<T> type, Path file) {
        ensureOpen();
        T instance = pipeline.load(type, file, LoadOrigin.USER);
        watchLoaded(type, file);
        return instance;
    }

    public <T> CompletableFuture<T> loadAsync(Class<T> type) {
        ensureOpen();
        return async.supply(() -> load(type));
    }

    public void save(Object instance) {
        ensureOpen();
        pipeline.save(instance, null);
    }

    public void save(Object instance, Path file) {
        ensureOpen();
        pipeline.save(instance, file);
    }

    public CompletableFuture<Void> saveAsync(Object instance) {
        ensureOpen();
        return async.run(() -> save(instance));
    }

    public Section open(Path file) {
        return open(file, Formats.AUTO);
    }

    public Section open(Path file, Formats format) {
        ensureOpen();
        if (file == null) {
            throw new NullPointerException("file");
        }
        Section section = pipeline.open(file, format);
        watch(file);
        return section;
    }

    public void save(Section section, Path file) {
        save(section, file, Formats.AUTO);
    }

    public void save(Section section, Path file, Formats format) {
        ensureOpen();
        pipeline.save(section, file, format);
    }

    public void save(FluentDocument document, Path file) {
        save(document, file, Formats.AUTO);
    }

    public void save(FluentDocument document, Path file, Formats format) {
        if (document == null) {
            throw new NullPointerException("document");
        }
        document.validate(new ValidationEngine()).throwIfInvalid();
        save(document.section(), file, format);
    }

    public MigrationBuilder migrations() {
        ensureOpen();
        return new MigrationBuilder(migrations);
    }

    public Subscription listen(ConfigListener listener) {
        ensureOpen();
        return events.subscribe(listener);
    }

    public void watch(Path file) {
        ensureOpen();
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (!config.watching()) {
            return;
        }
        Path absolute = file.toAbsolutePath().normalize();
        if (!watched.add(absolute)) {
            return;
        }
        watcher.watch(absolute, this::onChange);
    }

    @SuppressWarnings("unchecked")
    public <T> T latest(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        Object instance = instances.instance(type);
        if (instance == null) {
            return null;
        }
        return (T) type.cast(instance);
    }

    public Format format(Formats id) {
        return formats.require(id);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        try {
            watcher.close();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            async.close();
        } catch (RuntimeException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        documents.invalidate();
        if (failure != null) {
            throw failure;
        }
    }

    private void watchLoaded(Class<?> type, Path override) {
        if (!config.watching()) {
            return;
        }
        if (override != null) {
            watch(override);
            return;
        }
        String file = mapper.binding(type).file();
        if (file == null || Texts.blank(file)) {
            return;
        }
        Path raw = Paths.get(file);
        Path path = raw.isAbsolute()
                ? raw.normalize()
                : config.directory().resolve(raw).toAbsolutePath().normalize();
        watch(path);
    }

    private void onChange(Path path) {
        if (instances.suppressed(path)) {
            return;
        }
        Class<?> type = instances.type(path);
        if (!config.autoReload() || type == null) {
            safe(ConfigEvent.changed(path));
            return;
        }
        try {
            pipeline.load(type, path, LoadOrigin.RELOAD);
        } catch (RuntimeException exception) {
            safe(ConfigEvent.failed(path, type, exception));
        }
    }

    private void safe(ConfigEvent event) {
        try {
            events.publish(event);
        } catch (RuntimeException exception) {
            return;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("confine is closed");
        }
    }
}
