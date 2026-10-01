package confine.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Block extends Node {

    private final LinkedHashMap<String, Node> children = new LinkedHashMap<>();
    private final List<Node> order = new ArrayList<>();

    public Node get(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return children.get(key);
    }

    public boolean has(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return children.containsKey(key);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(children.keySet());
    }

    public List<Node> ordered() {
        return Collections.unmodifiableList(order);
    }

    public int size() {
        return children.size();
    }

    public boolean isEmpty() {
        return children.isEmpty() && order.stream().noneMatch(node -> node instanceof Note);
    }

    public void put(String key, Node child) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key");
        }
        if (child == null) {
            throw new NullPointerException("child");
        }
        if (child == this) {
            throw new IllegalArgumentException("cycle");
        }
        child.unlink();
        child.reparent(this, key);
        Node previous = children.put(key, child);
        if (previous == null) {
            order.add(child);
            return;
        }
        int index = order.indexOf(previous);
        if (index < 0) {
            order.add(child);
        } else {
            order.set(index, child);
        }
        previous.reparent(null, null);
    }

    public void add(Note comment) {
        if (comment == null) {
            throw new NullPointerException("comment");
        }
        comment.unlink();
        comment.reparent(this, null);
        order.add(comment);
    }

    public Node remove(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        Node existing = children.remove(key);
        if (existing == null) {
            return null;
        }
        order.remove(existing);
        existing.reparent(null, null);
        return existing;
    }

    public boolean detach(Node node) {
        if (node == null) {
            return false;
        }
        boolean removed = false;
        if (node.name() != null && children.get(node.name()) == node) {
            children.remove(node.name());
            removed = true;
        }
        if (order.remove(node)) {
            removed = true;
        }
        if (removed) {
            node.reparent(null, null);
        }
        return removed;
    }

    @Override
    public Shape kind() {
        return Shape.SECTION;
    }

    @Override
    public Block copy() {
        Block copy = new Block();
        copyMetaTo(copy);
        for (Node child : order) {
            if (child instanceof Note) {
                Note commentNode = (Note) child;
                copy.add(commentNode.copy());
                continue;
            }
            copy.put(child.name(), child.copy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Block)) {
            return false;
        }
        Block sectionNode = (Block) other;
        return sameMeta(sectionNode) && order.equals(sectionNode.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metaHash(), order);
    }

    @Override
    public String toString() {
        return "section(" + children.size() + ")";
    }
}
