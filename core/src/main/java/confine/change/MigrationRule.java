package confine.change;

public interface MigrationRule {

    int fromVersion();

    int toVersion();

    void apply(MigrationContext context);
}
