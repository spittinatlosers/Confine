package confine;

import confine.bind.ClassBinding;
import confine.bind.ConfigMapper;
import confine.bind.SchemaGenerator;
import confine.change.AnnotationMigrationSource;
import confine.change.BackupService;
import confine.change.MigrationEngine;
import confine.change.MigrationRegistry;
import confine.change.MigrationResult;
import confine.change.MigrationRule;
import confine.change.VersionKeys;
import confine.check.DefaultMerger;
import confine.check.MergePolicy;
import confine.check.ValidationEngine;
import confine.check.ValidationResult;
import confine.internal.Texts;
import confine.io.ConfigEvent;
import confine.io.ConfigEventBus;
import confine.io.ConfigEventType;
import confine.io.ConfigStore;
import confine.io.ResourceReader;
import confine.node.Block;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class DocumentPipeline {

    private final Settings config;
    private final Registry formats;
    private final ConfigMapper mapper;
    private final SchemaGenerator schemas;
    private final MigrationEngine migrations;
    private final MigrationRegistry migrationRegistry;
    private final AnnotationMigrationSource annotationMigrations;
    private final ValidationEngine validation;
    private final DefaultMerger merger;
    private final ConfigStore store;
    private final ResourceReader resources;
    private final BackupService backups;
    private final DocumentCache documents;
    private final ConfigEventBus events;
    private final InstanceRegistry instances;
    private final ConcurrentHashMap<Path, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    public DocumentPipeline(
            Settings config,
            Registry formats,
            ConfigMapper mapper,
            SchemaGenerator schemas,
            MigrationEngine migrations,
            MigrationRegistry migrationRegistry,
            AnnotationMigrationSource annotationMigrations,
            ValidationEngine validation,
            DefaultMerger merger,
            ConfigStore store,
            ResourceReader resources,
            BackupService backups,
            DocumentCache documents,
            ConfigEventBus events,
            InstanceRegistry instances) {
        this.config = config;
        this.formats = formats;
        this.mapper = mapper;
        this.schemas = schemas;
        this.migrations = migrations;
        this.migrationRegistry = migrationRegistry;
        this.annotationMigrations = annotationMigrations;
        this.validation = validation;
        this.merger = merger;
        this.store = store;
        this.resources = resources;
        this.backups = backups;
        this.documents = documents;
        this.events = events;
        this.instances = instances;
    }

    public <T> T load(Class<T> type, Path override, LoadOrigin origin) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        ClassBinding binding = mapper.binding(type);
        if (!binding.hasFile() && override == null) {
            throw new IllegalStateException(type.getName() + " is missing @Config");
        }
        Path path = resolve(binding.file(), override);
        ReentrantReadWriteLock lock = locks.computeIfAbsent(path, key -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            return loadLocked(type, binding, path, origin);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Section open(Path file, Formats format) {
        Path path = resolve(null, file);
        Format selected = formats.resolve(format, path, config.defaultFormat());
        ReentrantReadWriteLock lock = locks.computeIfAbsent(path, key -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            return Memory.wrap(openLocked(path, selected, file.getFileName().toString()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void save(Object instance, Path override) {
        if (instance == null) {
            throw new NullPointerException("instance");
        }
        ClassBinding binding = mapper.binding(instance.getClass());
        if (!binding.hasFile() && override == null) {
            throw new IllegalStateException(instance.getClass().getName() + " is missing @Config");
        }
        Path path = resolve(binding.file(), override);
        Format format = formats.resolve(binding.format(), path, config.defaultFormat());
        ReentrantReadWriteLock lock = locks.computeIfAbsent(path, key -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            Block written = mapper.write(instance);
            if (Files.exists(path)) {
                Block existing = store.read(path, format, config.charset());
                written = merger.overlay(existing, written);
            }
            suppress(path);
            store.write(path, written, format, config.charset());
            instances.track(path, instance.getClass(), instance);
            events.publish(ConfigEvent.saved(path, instance.getClass(), instance));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void save(Section section, Path file, Formats format) {
        if (section == null) {
            throw new NullPointerException("section");
        }
        if (file == null) {
            throw new NullPointerException("file");
        }
        Path path = resolve(null, file);
        Format selected = formats.resolve(format, path, config.defaultFormat());
        ReentrantReadWriteLock lock = locks.computeIfAbsent(path, key -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            suppress(path);
            store.write(path, section.node(), selected, config.charset());
            events.publish(ConfigEvent.saved(path, null, section));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> T loadLocked(Class<T> type, ClassBinding binding, Path path, LoadOrigin origin) {
        Format format = formats.resolve(binding.format(), path, config.defaultFormat());
        boolean exists = Files.exists(path);
        Block user = exists ? store.read(path, format, config.charset()) : new Block();
        Block baseline = user.copy();
        Block defaults = defaults(type, binding, format);
        int annotated = Math.max(binding.version(), 1);
        int defaultsVersion = VersionKeys.read(defaults, binding.versionKey(), annotated);
        int target = Math.max(annotated, defaultsVersion);
        int userVersion = exists ? VersionKeys.read(user, binding.versionKey(), 1) : 0;
        Path backup = null;
        MigrationResult migration = MigrationResult.unchanged(userVersion);
        if (exists && userVersion < target) {
            if (config.backupBeforeMigration()) {
                backup = backups.backup(path, userVersion);
            }
            migration = migrations.migrate(
                    user,
                    userVersion,
                    target,
                    binding.versionKey(),
                    rules(type, binding, path),
                    path);
            if (backup != null) {
                migration = migration.withBackup(backup);
            }
        }
        Block merged;
        if (!exists) {
            merged = defaults.copy();
        } else if (config.fillDefaults()) {
            merged = merger.merge(user, defaults, config.mergePolicy());
        } else {
            merged = user;
        }
        if (!exists || userVersion <= target) {
            VersionKeys.write(merged, binding.versionKey(), target);
        }
        ValidationResult result = validation.validate(merged, binding);
        if (!result.valid()) {
            safePublish(ConfigEvent.validationFailed(path, type, result.errors()));
            if (config.failOnValidation()) {
                result.throwIfInvalid();
            }
        }
        boolean changed = !exists || !baseline.equals(merged);
        boolean saved = false;
        if (result.valid() && changed && config.saveOnChange()) {
            suppress(path);
            store.write(path, merged, format, config.charset());
            saved = true;
        }
        T instance = mapper.read(type, merged);
        instances.track(path, type, instance);
        if (backup != null) {
            safePublish(ConfigEvent.backup(path, migration));
        }
        if (migration.migrated()) {
            safePublish(ConfigEvent.migrated(path, type, migration));
        }
        if (saved) {
            safePublish(ConfigEvent.saved(path, type, instance));
        }
        if (origin == LoadOrigin.RELOAD) {
            safePublish(ConfigEvent.reloaded(path, type, instance));
        } else {
            safePublish(ConfigEvent.loaded(path, type, instance));
        }
        return instance;
    }

    private Block openLocked(Path path, Format format, String fileName) {
        boolean exists = Files.exists(path);
        Block user = exists ? store.read(path, format, config.charset()) : new Block();
        Block baseline = user.copy();
        Optional<Block> resource = resource(fileName, format);
        Block defaults = resource.orElseGet(Block::new);
        int userVersion = exists ? VersionKeys.read(user, config.versionKey(), 1) : 0;
        int target = resource
                .map(node -> VersionKeys.read(node, config.versionKey(), userVersion))
                .orElse(userVersion);
        MigrationResult migration = MigrationResult.unchanged(userVersion);
        Path backup = null;
        if (exists && userVersion < target) {
            if (config.backupBeforeMigration()) {
                backup = backups.backup(path, userVersion);
            }
            migration = migrations.migrate(
                    user,
                    userVersion,
                    target,
                    config.versionKey(),
                    migrationRegistry.rules(null, fileName),
                    path);
            if (backup != null) {
                migration = migration.withBackup(backup);
            }
        }
        Block merged;
        if (!exists && resource.isPresent()) {
            merged = defaults.copy();
        } else if (exists && config.fillDefaults() && resource.isPresent()) {
            merged = merger.merge(user, defaults, config.mergePolicy());
        } else if (!exists) {
            merged = new Block();
        } else {
            merged = user;
        }
        if (target > 0 && userVersion <= target) {
            VersionKeys.write(merged, config.versionKey(), target);
        }
        boolean changed = !exists || !baseline.equals(merged);
        if (changed && config.saveOnChange() && (exists || resource.isPresent())) {
            suppress(path);
            store.write(path, merged, format, config.charset());
            safePublish(ConfigEvent.saved(path, null, null));
        }
        if (backup != null) {
            safePublish(ConfigEvent.backup(path, migration));
        }
        if (migration.migrated()) {
            safePublish(ConfigEvent.migrated(path, null, migration));
        }
        return merged;
    }

    private Block defaults(Class<?> type, ClassBinding binding, Format format) {
        Block schema = schemas.generate(type);
        Optional<Block> resource = resource(binding.file(), format);
        if (!resource.isPresent()) {
            return schema;
        }
        return merger.merge(resource.get(), schema, MergePolicy.FILL_MISSING_WITH_COMMENTS);
    }

    private Optional<Block> resource(String fileName, Format format) {
        if (fileName == null || Texts.blank(fileName)) {
            return Optional.empty();
        }
        String key = format.id() + ":" + config.resourceRoot() + ":" + fileName;
        Block cached = documents.get(key, () -> resources
                .read(config.resourceRoot(), fileName, format)
                .orElse(null));
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(cached);
    }

    private List<MigrationRule> rules(Class<?> type, ClassBinding binding, Path path) {
        List<MigrationRule> rules = new ArrayList<>();
        rules.addAll(annotationMigrations.from(type));
        String fileName = path.getFileName() == null
                ? binding.file()
                : path.getFileName().toString();
        rules.addAll(migrationRegistry.rules(type, fileName));
        if (!binding.file().equals(fileName)) {
            rules.addAll(migrationRegistry.rules(type, binding.file()));
        }
        return rules;
    }

    private Path resolve(String configured, Path override) {
        if (override != null) {
            return override.toAbsolutePath().normalize();
        }
        Path raw = Paths.get(configured);
        if (raw.isAbsolute()) {
            return raw.normalize();
        }
        return config.directory().resolve(raw).toAbsolutePath().normalize();
    }

    private void suppress(Path path) {
        long millis = Math.max(config.watchDebounceMillis(), 50L) * 3L + 250L;
        instances.suppress(path, TimeUnit.MILLISECONDS.toNanos(millis));
    }

    private void safePublish(ConfigEvent event) {
        try {
            events.publish(event);
        } catch (RuntimeException exception) {
            if (config.failOnValidation() && event.type() == ConfigEventType.VALIDATION_FAILED) {
                throw exception;
            }
        }
    }
}
