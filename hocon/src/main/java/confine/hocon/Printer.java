package confine.hocon;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

final class Printer {

    private final StringBuilder builder = new StringBuilder();

    String write(Block root) {
        writeComments(root.comments(), 0);
        writeObject(root, 0, false);
        return builder.toString();
    }

    private void writeObject(Block block, int indent, boolean nested) {
        for (Node child : block.ordered()) {
            if (child instanceof Note) {
                Note note = (Note) child;
                writeComment(note.text(), indent);
                continue;
            }
            if (child.name() == null) {
                continue;
            }
            writeComments(child.comments(), indent);
            builder.append(Texts.spaces(indent)).append(child.name());
            if (child instanceof Block) {
                Block nestedBlock = (Block) child;
                builder.append(" {");
                writeInline(nestedBlock.inlineComment());
                builder.append('\n');
                writeObject(nestedBlock, indent + 2, true);
                builder.append(Texts.spaces(indent)).append("}\n");
                continue;
            }
            builder.append(" = ");
            writeValue(child, indent);
            writeInline(child.inlineComment());
            builder.append('\n');
        }
        if (!nested && builder.length() == 0) {
            builder.append('\n');
        }
    }

    private void writeValue(Node node, int indent) {
        if (node instanceof Value) {
            Value value = (Value) node;
            writeScalar(value.value());
            return;
        }
        if (node instanceof Items) {
            Items items = (Items) node;
            builder.append('[');
            boolean first = true;
            for (Node element : items.elements()) {
                if (element instanceof Note) {
                    continue;
                }
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                if (element instanceof Block) {
                    Block block = (Block) element;
                    builder.append('{');
                    builder.append('\n');
                    writeObject(block, indent + 2, true);
                    builder.append(Texts.spaces(indent + 2)).append('}');
                    continue;
                }
                writeValue(element, indent);
            }
            builder.append(']');
            return;
        }
        throw new IllegalStateException("cannot write hocon node");
    }

    private void writeScalar(Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigInteger
                || value instanceof Short
                || value instanceof Byte) {
            builder.append(value);
            return;
        }
        if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            builder.append(value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value);
            return;
        }
        String text = String.valueOf(value);
        if (plain(text)) {
            builder.append(text);
            return;
        }
        builder.append('"').append(Texts.escape(text)).append('"');
    }

    private boolean plain(String text) {
        if (text.isEmpty()) {
            return false;
        }
        if (text.equals("true") || text.equals("false") || text.equals("null")) {
            return false;
        }
        if (Character.isWhitespace(text.charAt(0))
                || Character.isWhitespace(text.charAt(text.length() - 1))) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '{'
                    || current == '}'
                    || current == '['
                    || current == ']'
                    || current == ','
                    || current == '#'
                    || current == '='
                    || current == ':'
                    || current == '"'
                    || current == '\\'
                    || current < 0x20) {
                return false;
            }
        }
        return true;
    }

    private void writeComments(List<String> comments, int indent) {
        for (String comment : comments) {
            writeComment(comment, indent);
        }
    }

    private void writeComment(String comment, int indent) {
        builder.append(Texts.spaces(indent)).append("# ").append(comment).append('\n');
    }

    private void writeInline(String comment) {
        if (comment != null && !comment.isEmpty()) {
            builder.append(" # ").append(comment);
        }
    }
}
