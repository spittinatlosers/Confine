package confine.change;

import confine.internal.Texts;
import confine.node.Node;
import confine.node.Nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class TransformRule implements MigrationRule {

    private final int fromVersion;
    private final int toVersion;
    private final String path;
    private final Function<Object, Object> transformer;

    public TransformRule(
            int fromVersion,
            int toVersion,
            String path,
            Function<Object,
            Object> transformer) {
        if (fromVersion < 0 || toVersion <= fromVersion) {
            throw new IllegalArgumentException("version");
        }
        if (path == null || Texts.blank(path)) {
            throw new IllegalArgumentException("path");
        }
        if (transformer == null) {
            throw new NullPointerException("transformer");
        }
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.path = path;
        this.transformer = transformer;
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
        Node node = context.find(path);
        if (node == null) {
            return;
        }
        List<String> comments = new ArrayList<>(node.comments());
        String inline = node.inlineComment();
        Object updated;
        try {
            updated = transformer.apply(Nodes.plain(node));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("transform failed at " + path, exception);
        }
        Node written = Nodes.fromPlain(updated);
        written.comments().addAll(comments);
        written.setInlineComment(inline);
        context.set(path, written);
    }
}
