package confine.change;

import confine.internal.Texts;

public final class MoveRule implements MigrationRule {

    private final int fromVersion;
    private final int toVersion;
    private final String fromPath;
    private final String toPath;

    public MoveRule(int fromVersion, int toVersion, String fromPath, String toPath) {
        if (fromVersion < 0 || toVersion <= fromVersion) {
            throw new IllegalArgumentException("version");
        }
        if (fromPath == null || Texts.blank(fromPath) || toPath == null || Texts.blank(toPath)) {
            throw new IllegalArgumentException("path");
        }
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.fromPath = fromPath;
        this.toPath = toPath;
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
        context.move(fromPath, toPath);
    }
}
