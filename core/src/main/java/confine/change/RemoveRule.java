package confine.change;

import confine.internal.Texts;

public final class RemoveRule implements MigrationRule {

    private final int fromVersion;
    private final int toVersion;
    private final String path;

    public RemoveRule(int fromVersion, int toVersion, String path) {
        if (fromVersion < 0 || toVersion <= fromVersion) {
            throw new IllegalArgumentException("version");
        }
        if (path == null || Texts.blank(path)) {
            throw new IllegalArgumentException("path");
        }
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.path = path;
    }

    @Override
    public int fromVersion() {
        return fromVersion;
    }

    @Override
    public int toVersion() {
        return toVersion;
    }

    @Override
    public void apply(MigrationContext context) {
        context.remove(path);
    }
}
