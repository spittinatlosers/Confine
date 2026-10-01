package confine.change;

import confine.internal.Texts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class MigrationBuilder {

    private final MigrationRegistry registry;
    private String file;
    private Class<?> type;
    private final List<MigrationRule> rules = new ArrayList<>();

    public MigrationBuilder(MigrationRegistry registry) {
        if (registry == null) {
            throw new NullPointerException("registry");
        }
        this.registry = registry;
    }

    public MigrationBuilder file(String file) {
        if (file == null || Texts.blank(file)) {
            throw new IllegalArgumentException("file");
        }
        this.file = file;
        return this;
    }

    public MigrationBuilder type(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.type = type;
        return this;
    }

    public StepBuilder from(int version) {
        return new StepBuilder(this, version);
    }

    public void add(MigrationRule rule) {
        if (rule == null) {
            throw new NullPointerException("rule");
        }
        rules.add(rule);
    }

    public void register() {
        if (file == null && type == null) {
            throw new IllegalStateException("migration target is required");
        }
        if (file != null) {
            registry.addFile(file, rules);
        }
        if (type != null) {
            registry.addType(type, rules);
        }
    }

    public static final class StepBuilder {

        private final MigrationBuilder parent;
        private final int from;
        private int to;
        private final List<Pending> pending = new ArrayList<>();

        StepBuilder(MigrationBuilder parent, int from) {
            if (from < 0) {
                throw new IllegalArgumentException("from");
            }
            this.parent = parent;
            this.from = from;
            this.to = from + 1;
        }

        public StepBuilder to(int version) {
            if (version <= from) {
                throw new IllegalArgumentException("to");
            }
            this.to = version;
            return this;
        }

        public StepBuilder rename(String fromPath, String toPath) {
            pending.add(new Pending(Kind.RENAME, fromPath, toPath, null));
            return this;
        }

        public StepBuilder move(String fromPath, String toPath) {
            pending.add(new Pending(Kind.MOVE, fromPath, toPath, null));
            return this;
        }

        public StepBuilder remove(String path) {
            pending.add(new Pending(Kind.REMOVE, path, null, null));
            return this;
        }

        public StepBuilder copy(String fromPath, String toPath) {
            pending.add(new Pending(Kind.COPY, fromPath, toPath, null));
            return this;
        }

        public StepBuilder transform(String path, Function<Object, Object> transformer) {
            if (transformer == null) {
                throw new NullPointerException("transformer");
            }
            pending.add(new Pending(Kind.TRANSFORM, path, null, transformer));
            return this;
        }

        public MigrationBuilder end() {
            for (Pending item : pending) {
                parent.add(item.rule(from, to));
            }
            return parent;
        }
    }

    private enum Kind {
        RENAME,
        MOVE,
        REMOVE,
        COPY,
        TRANSFORM
    }

    private static final class Pending {

        private final Kind kind;
        private final String fromPath;
        private final String toPath;
        private final Function<Object, Object> transformer;

        private Pending(
                Kind kind,
                String fromPath,
                String toPath,
                Function<Object,
                Object> transformer) {
            this.kind = kind;
            this.fromPath = fromPath;
            this.toPath = toPath;
            this.transformer = transformer;
        }

        private MigrationRule rule(int from, int to) {
            switch (kind) {
                case RENAME:
                    return new RenameRule(from, to, fromPath, toPath);
                case MOVE:
                    return new MoveRule(from, to, fromPath, toPath);
                case REMOVE:
                    return new RemoveRule(from, to, fromPath);
                case COPY:
                    return new CopyRule(from, to, fromPath, toPath);
                case TRANSFORM:
                    return new TransformRule(from, to, fromPath, transformer);
                default:
                    throw new IllegalStateException("migration");
            }
        }
    }
}
