package confine.change;

import java.nio.file.Path;
import java.util.Objects;

public final class MigrationResult {

    public enum Status {
        UNCHANGED,
        MIGRATED,
        NEWER
    }

    private final Status status;
    private final int fromVersion;
    private final int toVersion;
    private final int appliedRules;
    private final Path backup;

    private MigrationResult(
            Status status,
            int fromVersion,
            int toVersion,
            int appliedRules,
            Path backup) {
        this.status = status;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.appliedRules = appliedRules;
        this.backup = backup;
    }

    public static MigrationResult unchanged(int version) {
        return new MigrationResult(Status.UNCHANGED, version, version, 0, null);
    }

    public static MigrationResult newer(int fromVersion, int toVersion) {
        return new MigrationResult(Status.NEWER, fromVersion, toVersion, 0, null);
    }

    public static MigrationResult migrated(
            int fromVersion,
            int toVersion,
            int appliedRules,
            Path backup) {
        return new MigrationResult(Status.MIGRATED, fromVersion, toVersion, appliedRules, backup);
    }

    public Status status() {
        return status;
    }

    public int fromVersion() {
        return fromVersion;
    }

    public int toVersion() {
        return toVersion;
    }

    public int appliedRules() {
        return appliedRules;
    }

    public Path backup() {
        return backup;
    }

    public boolean migrated() {
        return status == Status.MIGRATED;
    }

    public MigrationResult withBackup(Path backup) {
        return new MigrationResult(status, fromVersion, toVersion, appliedRules, backup);
    }

    @Override
    public String toString() {
        return status + " " + fromVersion + "->" + toVersion + " rules=" + appliedRules;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MigrationResult)) {
            return false;
        }
        MigrationResult result = (MigrationResult) other;
        return status == result.status
                && fromVersion == result.fromVersion
                && toVersion == result.toVersion
                && appliedRules == result.appliedRules
                && Objects.equals(backup, result.backup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, fromVersion, toVersion, appliedRules, backup);
    }
}
