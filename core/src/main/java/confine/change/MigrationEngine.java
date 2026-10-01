package confine.change;

import confine.node.Block;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MigrationEngine {

    public MigrationResult migrate(
            Block document,
            int fromVersion,
            int targetVersion,
            String versionKey,
            List<MigrationRule> rules,
            Path file) {
        if (document == null) {
            throw new NullPointerException("document");
        }
        if (rules == null) {
            throw new NullPointerException("rules");
        }
        if (fromVersion == targetVersion) {
            return MigrationResult.unchanged(fromVersion);
        }
        if (fromVersion > targetVersion) {
            return MigrationResult.newer(fromVersion, targetVersion);
        }
        List<MigrationRule> ordered = new ArrayList<>(rules);
        ordered.sort(Comparator
                .comparingInt(MigrationRule::fromVersion)
                .thenComparingInt(MigrationRule::toVersion));
        Map<String, Object> attributes = new LinkedHashMap<>();
        int version = fromVersion;
        int applied = 0;
        int guard = 0;
        while (version < targetVersion) {
            if (guard++ > 10000) {
                throw new IllegalStateException("migration did not advance past version " + version);
            }
            List<MigrationRule> step = new ArrayList<>();
            for (MigrationRule rule : ordered) {
                if (rule.fromVersion() == version) {
                    step.add(rule);
                }
            }
            int next = version + 1;
            for (MigrationRule rule : step) {
                if (rule.toVersion() > next) {
                    next = rule.toVersion();
                }
            }
            if (next > targetVersion) {
                next = targetVersion;
            }
            if (next <= version) {
                throw new IllegalStateException("migration stalled at version " + version);
            }
            MigrationContext context = new MigrationContext(
                    document,
                    file,
                    version,
                    next,
                    attributes);
            for (MigrationRule rule : step) {
                rule.apply(context);
                applied++;
            }
            version = next;
        }
        VersionKeys.write(document, versionKey, targetVersion);
        return MigrationResult.migrated(fromVersion, targetVersion, applied, null);
    }
}
