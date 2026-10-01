package confine.change;

import confine.Migration;
import confine.internal.Texts;

import java.util.ArrayList;
import java.util.List;

public final class AnnotationMigrationSource {

    public List<MigrationRule> from(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        List<MigrationRule> rules = new ArrayList<>();
        for (Migration annotation : type.getAnnotationsByType(Migration.class)) {
            int from = annotation.from();
            int to = annotation.to() < 0 ? from + 1 : annotation.to();
            if (to <= from) {
                throw new IllegalStateException("migration on "
                        + type.getName()
                        + " does not advance");
            }
            for (String rename : annotation.rename()) {
                String[] parts = arrow(rename);
                rules.add(new RenameRule(from, to, parts[0], parts[1]));
            }
            for (String move : annotation.move()) {
                String[] parts = arrow(move);
                rules.add(new MoveRule(from, to, parts[0], parts[1]));
            }
            for (String path : annotation.remove()) {
                if (path == null || Texts.blank(path)) {
                    throw new IllegalStateException("empty remove path");
                }
                rules.add(new RemoveRule(from, to, path.trim()));
            }
        }
        return rules;
    }

    private String[] arrow(String value) {
        if (value == null) {
            throw new IllegalStateException("missing migration path");
        }
        int index = value.indexOf("->");
        if (index < 0) {
            throw new IllegalStateException("expected -> in " + value);
        }
        String left = value.substring(0, index).trim();
        String right = value.substring(index + 2).trim();
        if (left.isEmpty() || right.isEmpty()) {
            throw new IllegalStateException("expected -> in " + value);
        }
        return new String[] {left, right};
    }
}
