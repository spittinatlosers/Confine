package confine.change;

import confine.Memory;
import confine.Section;
import confine.internal.Texts;
import confine.node.Block;
import confine.node.Node;
import confine.node.Nodes;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MigrationContext {

    private final Block document;
    private final Path file;
    private final int fromVersion;
    private final int toVersion;
    private final Map<String, Object> attributes;

    public MigrationContext(
            Block document,
            Path file,
            int fromVersion,
            int toVersion,
            Map<String,
            Object> attributes) {
        if (document == null) {
            throw new NullPointerException("document");
        }
        this.document = document;
        this.file = file;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.attributes = attributes == null ? new LinkedHashMap<>() : attributes;
    }

    public Block document() {
        return document;
    }

    public Path file() {
        return file;
    }

    public int fromVersion() {
        return fromVersion;
    }

    public int toVersion() {
        return toVersion;
    }

    public Section section() {
        return Memory.wrap(document);
    }

    public Object attribute(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return attributes.get(key);
    }

    public void attribute(String key, Object value) {
        if (key == null || Texts.blank(key)) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            attributes.remove(key);
            return;
        }
        attributes.put(key, value);
    }

    public Node find(String path) {
        return Nodes.find(document, path);
    }

    public void set(String path, Node node) {
        Nodes.set(document, path, node);
    }

    public void set(String path, Object value) {
        Nodes.set(document, path, Nodes.fromPlain(value));
    }

    public Node remove(String path) {
        return Nodes.remove(document, path);
    }

    public void move(String from, String to) {
        if (from == null || to == null) {
            throw new NullPointerException("path");
        }
        if (from.equals(to) || to.startsWith(from + ".")) {
            throw new IllegalStateException("cannot move " + from + " into " + to);
        }
        Node node = Nodes.remove(document, from);
        if (node == null) {
            return;
        }
        try {
            Nodes.set(document, to, node);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("cannot move " + from + " to " + to, exception);
        }
    }

    public void copy(String from, String to) {
        Node node = Nodes.find(document, from);
        if (node == null) {
            return;
        }
        Nodes.set(document, to, node.copy());
    }
}
