package confine.json;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

final class Json5Writer {

    private final boolean strict;
    private final StringBuilder builder = new StringBuilder();

    Json5Writer(boolean strict) {
        this.strict = strict;
    }

    String write(Block root) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        writeComments(root.comments(), 0);
        builder.append('{');
        boolean hasChild = hasContent(root);
        if (hasChild) {
            builder.append('\n');
            writeSectionBody(root, 1);
            builder.append('}');
        } else {
            builder.append('}');
        }
        builder.append('\n');
        return builder.toString();
    }

    private void writeSectionBody(Block section, int indent) {
        boolean pending = false;
        for (Node child : section.ordered()) {
            if (child instanceof Note) {
                Note commentNode = (Note) child;
                if (strict) {
                    continue;
                }
                writeComments(Arrays.asList(commentNode.text()), indent);
                continue;
            }
            if (child.name() == null) {
                continue;
            }
            if (pending) {
                builder.append(",\n");
            }
            writeComments(child.comments(), indent);
            indent(indent);
            writeKey(child.name());
            builder.append(": ");
            writeNode(child, indent);
            writeInline(child.inlineComment());
            pending = true;
        }
        if (pending) {
            builder.append('\n');
        }
    }

    private void writeNode(Node node, int indent) {
        if (node instanceof Block) {
            Block sectionNode = (Block) node;
            builder.append('{');
            if (hasContent(sectionNode)) {
                builder.append('\n');
                writeSectionBody(sectionNode, indent + 1);
                indent(indent);
                builder.append('}');
                return;
            }
            builder.append('}');
            return;
        }
        if (node instanceof Items) {
            Items listNode = (Items) node;
            writeList(listNode, indent);
            return;
        }
        if (node instanceof Value) {
            Value valueNode = (Value) node;
            writeScalar(valueNode.value());
            return;
        }
        throw new IllegalStateException("cannot write " + node.kind());
    }

    private void writeList(Items list, int indent) {
        builder.append('[');
        if (list.elements().isEmpty()) {
            builder.append(']');
            return;
        }
        builder.append('\n');
        boolean pending = false;
        for (Node element : list.elements()) {
            if (element instanceof Note) {
                Note commentNode = (Note) element;
                if (strict) {
                    continue;
                }
                writeComments(Arrays.asList(commentNode.text()), indent + 1);
                continue;
            }
            if (pending) {
                builder.append(",\n");
            }
            writeComments(element.comments(), indent + 1);
            indent(indent + 1);
            writeNode(element, indent + 1);
            writeInline(element.inlineComment());
            pending = true;
        }
        if (pending) {
            builder.append('\n');
        }
        indent(indent);
        builder.append(']');
    }

    private void writeScalar(Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Byte
                || value instanceof Short) {
            builder.append(value);
            return;
        }
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            builder.append(decimal.toPlainString());
            return;
        }
        if (value instanceof Float || value instanceof Double) {
            builder.append(value);
            return;
        }
        builder.append('"');
        builder.append(Texts.escape(String.valueOf(value)));
        builder.append('"');
    }

    private void writeKey(String key) {
        if (!strict && isIdentifier(key)) {
            builder.append(key);
            return;
        }
        builder.append('"');
        builder.append(Texts.escape(key));
        builder.append('"');
    }

    private void writeComments(List<String> comments, int indent) {
        if (strict || comments == null) {
            return;
        }
        for (String comment : comments) {
            indent(indent);
            builder.append("// ");
            builder.append(comment == null ? "" : comment);
            builder.append('\n');
        }
    }

    private void writeInline(String comment) {
        if (strict || comment == null || Texts.blank(comment)) {
            return;
        }
        builder.append(" // ");
        builder.append(comment.trim());
    }

    private void indent(int level) {
        builder.append(Texts.spaces(Math.max(level, 0) * 2));
    }

    private boolean hasContent(Block section) {
        for (Node child : section.ordered()) {
            if (child instanceof Note && strict) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean isIdentifier(String key) {
        if (key.isEmpty()) {
            return false;
        }
        char first = key.charAt(0);
        if (!(Character.isLetter(first) || first == '_' || first == '$')) {
            return false;
        }
        for (int index = 1; index < key.length(); index++) {
            char current = key.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '_' || current == '$')) {
                return false;
            }
        }
        return true;
    }
}
