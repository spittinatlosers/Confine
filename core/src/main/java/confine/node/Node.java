package confine.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Node {

    private Node parent;
    private String name;
    private final List<String> comments = new ArrayList<>();
    private String inlineComment;

    public abstract Shape kind();

    public abstract Node copy();

    public Node parent() {
        return parent;
    }

    public String name() {
        return name;
    }

    public List<String> comments() {
        return comments;
    }

    public String inlineComment() {
        return inlineComment;
    }

    public void setInlineComment(String inlineComment) {
        this.inlineComment = inlineComment;
    }

    public void addComment(String comment) {
        if (comment == null) {
            throw new NullPointerException("comment");
        }
        comments.add(comment);
    }

    void reparent(Node parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public final void unlink() {
        Node current = parent;
        if (current instanceof Block) {
            Block sectionNode = (Block) current;
            sectionNode.detach(this);
            return;
        }
        if (current instanceof Items) {
            Items listNode = (Items) current;
            listNode.detach(this);
            return;
        }
        parent = null;
        name = null;
    }

    protected final void copyMetaTo(Node target) {
        target.comments.addAll(comments);
        target.inlineComment = inlineComment;
        target.name = name;
    }

    protected final boolean sameMeta(Node other) {
        return Objects.equals(name, other.name)
                && Objects.equals(inlineComment, other.inlineComment)
                && comments.equals(other.comments);
    }

    protected final int metaHash() {
        return Objects.hash(name, inlineComment, comments);
    }
}
