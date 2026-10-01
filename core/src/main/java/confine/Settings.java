package confine;

import confine.bind.TypeAdapter;
import confine.check.MergePolicy;
import confine.internal.Texts;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Settings {

    private final Path directory;
    private final ClassLoader classLoader;
    private final String resourceRoot;
    private final boolean backupBeforeMigration;
    private final boolean saveOnChange;
    private final boolean autoReload;
    private final boolean watchEnabled;
    private final boolean fillDefaults;
    private final boolean failOnValidation;
    private final MergePolicy mergePolicy;
    private final String versionKey;
    private final int ioThreads;
    private final long watchDebounceMillis;
    private final long cacheMaximumSize;
    private final Formats defaultFormat;
    private final Charset charset;
    private final List<TypeAdapter<?>> adapters;

    public Settings(
            Path directory,
            ClassLoader classLoader,
            String resourceRoot,
            boolean backupBeforeMigration,
            boolean saveOnChange,
            boolean autoReload,
            boolean watchEnabled,
            boolean fillDefaults,
            boolean failOnValidation,
            MergePolicy mergePolicy,
            String versionKey,
            int ioThreads,
            long watchDebounceMillis,
            long cacheMaximumSize,
            Formats defaultFormat,
            Charset charset,
            List<TypeAdapter<?>> adapters) {
        if (directory == null) {
            throw new NullPointerException("directory");
        }
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        if (mergePolicy == null) {
            throw new NullPointerException("mergePolicy");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.directory = directory;
        this.classLoader = classLoader;
        this.resourceRoot = resourceRoot == null ? "" : resourceRoot;
        this.backupBeforeMigration = backupBeforeMigration;
        this.saveOnChange = saveOnChange;
        this.autoReload = autoReload;
        this.watchEnabled = watchEnabled;
        this.fillDefaults = fillDefaults;
        this.failOnValidation = failOnValidation;
        this.mergePolicy = mergePolicy;
        this.versionKey = versionKey == null || Texts.blank(versionKey)
                ? "config-version"
                : versionKey;
        this.ioThreads = ioThreads;
        this.watchDebounceMillis = watchDebounceMillis;
        this.cacheMaximumSize = cacheMaximumSize;
        this.defaultFormat = defaultFormat == null ? Formats.YAML : defaultFormat;
        this.charset = charset;
        this.adapters = adapters == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(adapters));
    }

    public Path directory() {
        return directory;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public String resourceRoot() {
        return resourceRoot;
    }

    public boolean backupBeforeMigration() {
        return backupBeforeMigration;
    }

    public boolean saveOnChange() {
        return saveOnChange;
    }

    public boolean autoReload() {
        return autoReload;
    }

    public boolean watchEnabled() {
        return watchEnabled;
    }

    public boolean watching() {
        return watchEnabled || autoReload;
    }

    public boolean fillDefaults() {
        return fillDefaults;
    }

    public boolean failOnValidation() {
        return failOnValidation;
    }

    public MergePolicy mergePolicy() {
        return mergePolicy;
    }

    public String versionKey() {
        return versionKey;
    }

    public int ioThreads() {
        return ioThreads;
    }

    public long watchDebounceMillis() {
        return watchDebounceMillis;
    }

    public long cacheMaximumSize() {
        return cacheMaximumSize;
    }

    public Formats defaultFormat() {
        return defaultFormat;
    }

    public Charset charset() {
        return charset;
    }

    public List<TypeAdapter<?>> adapters() {
        return adapters;
    }
}
