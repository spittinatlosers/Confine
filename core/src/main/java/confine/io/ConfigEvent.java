package confine.io;

import confine.change.MigrationResult;
import confine.check.ValidationError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigEvent {

    private final ConfigEventType type;
    private final Path path;
    private final Class<?> configType;
    private final Object instance;
    private final MigrationResult migration;
    private final List<ValidationError> errors;
    private final Throwable failure;

    private ConfigEvent(
            ConfigEventType type,
            Path path,
            Class<?> configType,
            Object instance,
            MigrationResult migration,
            List<ValidationError> errors,
            Throwable failure) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.type = type;
        this.path = path;
        this.configType = configType;
        this.instance = instance;
        this.migration = migration;
        this.errors = errors == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(errors));
        this.failure = failure;
    }

    public static ConfigEvent loaded(Path path, Class<?> configType, Object instance) {
        return new ConfigEvent(
                ConfigEventType.LOADED,
                path,
                configType,
                instance,
                null,
                Collections.emptyList(),
                null);
    }

    public static ConfigEvent reloaded(Path path, Class<?> configType, Object instance) {
        return new ConfigEvent(
                ConfigEventType.RELOADED,
                path,
                configType,
                instance,
                null,
                Collections.emptyList(),
                null);
    }

    public static ConfigEvent saved(Path path, Class<?> configType, Object instance) {
        return new ConfigEvent(
                ConfigEventType.SAVED,
                path,
                configType,
                instance,
                null,
                Collections.emptyList(),
                null);
    }

    public static ConfigEvent migrated(Path path, Class<?> configType, MigrationResult migration) {
        return new ConfigEvent(
                ConfigEventType.MIGRATED,
                path,
                configType,
                null,
                migration,
                Collections.emptyList(),
                null);
    }

    public static ConfigEvent changed(Path path) {
        return new ConfigEvent(
                ConfigEventType.CHANGED,
                path,
                null,
                null,
                null,
                Collections.emptyList(),
                null);
    }

    public static ConfigEvent validationFailed(
            Path path,
            Class<?> configType,
            List<ValidationError> errors) {
        return new ConfigEvent(
                ConfigEventType.VALIDATION_FAILED,
                path,
                configType,
                null,
                null,
                errors,
                null);
    }

    public static ConfigEvent backup(Path path, MigrationResult migration) {
        return new ConfigEvent(
                ConfigEventType.BACKUP_CREATED,
                path,
                null,
                null,
                migration,
                Collections.emptyList(),
                null);
    }

    public static ConfigEvent failed(Path path, Class<?> configType, Throwable failure) {
        return new ConfigEvent(
                ConfigEventType.FAILED,
                path,
                configType,
                null,
                null,
                Collections.emptyList(),
                failure);
    }

    public ConfigEventType type() {
        return type;
    }

    public Path path() {
        return path;
    }

    public Class<?> configType() {
        return configType;
    }

    public Object instance() {
        return instance;
    }

    public MigrationResult migration() {
        return migration;
    }

    public List<ValidationError> errors() {
        return errors;
    }

    public Throwable failure() {
        return failure;
    }
}
