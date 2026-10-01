package confine.node;

import java.util.Objects;

public final class Note extends Node {

    private final String text;

    public Note(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public Shape kind() {
        return Shape.COMMENT;
    }

    @Override
    public Note copy() {
        Note copy = new Note(text);
        copyMetaTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Note)) {
            return false;
        }
        Note commentNode = (Note) other;
        return sameMeta(commentNode) && text.equals(commentNode.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metaHash(), text);
    }

    @Override
    public String toString() {
        return "comment(" + text + ")";
    }
}
