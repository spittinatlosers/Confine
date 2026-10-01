package confine.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Items extends Node {

    private final List<Node> elements = new ArrayList<>();

    public List<Node> elements() {
        return Collections.unmodifiableList(elements);
    }

    public int size() {
        return elements.size();
    }

    public Node get(int index) {
        return elements.get(index);
    }

    public void add(Node element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        if (element == this) {
            throw new IllegalArgumentException("cycle");
        }
        element.unlink();
        element.reparent(this, null);
        elements.add(element);
    }

    public Node set(int index, Node element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        if (element == this) {
            throw new IllegalArgumentException("cycle");
        }
        element.unlink();
        element.reparent(this, null);
        Node previous = elements.set(index, element);
        if (previous != null) {
            previous.reparent(null, null);
        }
        return previous;
    }

    public Node remove(int index) {
        Node removed = elements.remove(index);
        if (removed != null) {
            removed.reparent(null, null);
        }
        return removed;
    }

    public void detach(Node node) {
        if (node == null) {
            return;
        }
        if (elements.remove(node)) {
            node.reparent(null, null);
        }
    }

    @Override
    public Shape kind() {
        return Shape.LIST;
    }

    @Override
    public Items copy() {
        Items copy = new Items();
        copyMetaTo(copy);
        for (Node element : elements) {
            copy.add(element.copy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Items)) {
            return false;
        }
        Items listNode = (Items) other;
        return sameMeta(listNode) && elements.equals(listNode.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metaHash(), elements);
    }

    @Override
    public String toString() {
        return "list(" + elements.size() + ")";
    }
}
