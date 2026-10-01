package confine.change;

import confine.internal.Texts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MigrationRegistry {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MigrationRule>> byFile = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<MigrationRule>> byType = new ConcurrentHashMap<>();

    public void addFile(String file, List<MigrationRule> rules) {
        if (file == null || Texts.blank(file)) {
            throw new IllegalArgumentException("file");
        }
        add(byFile.computeIfAbsent(file, key -> new CopyOnWriteArrayList<>()), rules);
    }

    public void addType(Class<?> type, List<MigrationRule> rules) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        add(byType.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()), rules);
    }

    public List<MigrationRule> rules(Class<?> type, String fileName) {
        List<MigrationRule> rules = new ArrayList<>();
        if (type != null) {
            CopyOnWriteArrayList<MigrationRule> typed = byType.get(type);
            if (typed != null) {
                rules.addAll(typed);
            }
        }
        if (fileName != null) {
            CopyOnWriteArrayList<MigrationRule> named = byFile.get(fileName);
            if (named != null) {
                rules.addAll(named);
            }
        }
        return rules;
    }

    private void add(CopyOnWriteArrayList<MigrationRule> target, List<MigrationRule> rules) {
        if (rules == null) {
            return;
        }
        for (MigrationRule rule : rules) {
            if (rule == null) {
                throw new NullPointerException("rule");
            }
            target.add(rule);
        }
    }
}
