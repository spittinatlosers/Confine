package confine;

import confine.bind.TypeAdapter;
import confine.check.MergePolicy;
import confine.internal.Require;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class Builder {

    private Path directory = Paths.get(".");
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private String resourceRoot = "";
    private boolean backupBeforeMigration = true;
    private boolean saveOnChange = true;
    private boolean autoReload = false;
    private boolean watchEnabled = false;
    private boolean fillDefaults = true;
    private boolean failOnValidation = true;
    private MergePolicy mergePolicy = MergePolicy.FILL_MISSING_WITH_COMMENTS;
    private String versionKey = "config-version";
    private int ioThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
    private long watchDebounceMillis = 200L;
    private long cacheMaximumSize = 256L;
    private Formats defaultFormat = Formats.YAML;
    private Charset charset = StandardCharsets.UTF_8;
    private final List<TypeAdapter<?>> adapters = new ArrayList<>();

    public Builder directory(Path directory) {
        this.directory = Require.nonNull(directory, "directory");
        return this;
    }

    public Builder classLoader(ClassLoader classLoader) {
        this.classLoader = Require.nonNull(classLoader, "classLoader");
        return this;
    }

    public Builder resourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot == null ? "" : resourceRoot;
        return this;
    }

    public Builder backupBeforeMigration(boolean backupBeforeMigration) {
        this.backupBeforeMigration = backupBeforeMigration;
        return this;
    }

    public Builder saveOnChange(boolean saveOnChange) {
        this.saveOnChange = saveOnChange;
        return this;
    }

    public Builder autoReload(boolean autoReload) {
        this.autoReload = autoReload;
        return this;
    }

    public Builder watchEnabled(boolean watchEnabled) {
        this.watchEnabled = watchEnabled;
        return this;
    }

    public Builder fillDefaults(boolean fillDefaults) {
        this.fillDefaults = fillDefaults;
        return this;
    }

    public Builder failOnValidation(boolean failOnValidation) {
        this.failOnValidation = failOnValidation;
        return this;
    }

    public Builder mergePolicy(MergePolicy mergePolicy) {
        this.mergePolicy = Require.nonNull(mergePolicy, "mergePolicy");
        return this;
    }

    public Builder versionKey(String versionKey) {
        this.versionKey = Require.nonBlank(versionKey, "versionKey");
        return this;
    }

    public Builder ioThreads(int ioThreads) {
        this.ioThreads = Require.positive(ioThreads, "ioThreads");
        return this;
    }

    public Builder watchDebounceMillis(long watchDebounceMillis) {
        this.watchDebounceMillis = Require.notNegative(watchDebounceMillis, "watchDebounceMillis");
        return this;
    }

    public Builder cacheMaximumSize(long cacheMaximumSize) {
        this.cacheMaximumSize = Require.notNegative(cacheMaximumSize, "cacheMaximumSize");
        return this;
    }

    public Builder defaultFormat(Formats defaultFormat) {
        this.defaultFormat = Require.nonNull(defaultFormat, "defaultFormat");
        return this;
    }

    public Builder charset(Charset charset) {
        this.charset = Require.nonNull(charset, "charset");
        return this;
    }

    public Builder adapter(TypeAdapter<?> adapter) {
        this.adapters.add(Require.nonNull(adapter, "adapter"));
        return this;
    }

    public Confine build() {
        if (classLoader == null) {
            classLoader = Confine.class.getClassLoader();
        }
        if (ioThreads <= 0) {
            ioThreads = 1;
        }
        Settings config = new Settings(
                directory.toAbsolutePath().normalize(),
                classLoader,
                resourceRoot,
                backupBeforeMigration,
                saveOnChange,
                autoReload,
                watchEnabled,
                fillDefaults,
                failOnValidation,
                mergePolicy,
                versionKey,
                ioThreads,
                watchDebounceMillis,
                cacheMaximumSize,
                defaultFormat,
                charset,
                adapters);
        return new Confine(config);
    }
}
