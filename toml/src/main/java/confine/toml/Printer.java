package confine.toml;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

final class Printer {

    private final StringBuilder builder = new StringBuilder();

    String write(Block root) {
        writeComments(root.comments());
        writeInline(root.inlineComment());
        if (root.inlineComment() != null) {
            builder.append('\n');
        }
        writeBlock(root, "");
        return builder.toString();
    }

    private void writeBlock(Block block, String path) {
        List<Node> tables = new ArrayList<>();
        for (Node child : block.ordered()) {
            if (child instanceof Note) {
                Note note = (Note) child;
                builder.append("# ").append(note.text()).append('\n');
                continue;
            }
            if (child.name() == null) {
                continue;
            }
            if (child instanceof Value || (child instanceof Items && scalar(((Items) child)))) {
                writePair(child);
                continue;
            }
            tables.add(child);
        }
        for (Node child : tables) {
            String next = join(path, child.name());
            if (child instanceof Block) {
                Block nested = (Block) child;
                writeComments(nested.comments());
                builder.append('[').append(next).append(']');
                writeInline(nested.inlineComment());
                builder.append('\n');
                writeBlock(nested, next);
                continue;
            }
            if (child instanceof Items) {
                Items items = (Items) child;
                for (Node element : items.elements()) {
                    if (!(element instanceof Block)) {
                        throw new IllegalStateException("cannot write toml node");
                    }
                    Block row = (Block) element;
                    writeComments(row.comments());
                    builder.append("[[").append(next).append("]]");
                    writeInline(row.inlineComment());
                    builder.append('\n');
                    writeBlock(row, next);
                }
            }
        }
    }

    private void writePair(Node node) {
        writeComments(node.comments());
        builder.append(quote(node.name())).append(" = ");
        writeValue(node);
        writeInline(node.inlineComment());
        builder.append('\n');
    }

    private void writeValue(Node node) {
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
                writeValue(element);
            }
            builder.append(']');
            return;
        }
        if (node instanceof Block) {
            Block block = (Block) node;
            builder.append('{');
            boolean first = true;
            for (Node child : block.ordered()) {
                if (!(child instanceof Value) || child.name() == null) {
                    continue;
                }
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append(quote(child.name())).append(" = ");
                writeScalar(((Value) child).value());
            }
            builder.append('}');
            return;
        }
        throw new IllegalStateException("cannot write toml node");
    }

    private void writeScalar(Object value) {
        if (value == null) {
            throw new IllegalStateException("toml null is not supported");
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
        if (value instanceof Double) {
            Double number = (Double) value;
            if (number.isInfinite() || number.isNaN()) {
                builder.append(number.isNaN() ? "nan" : number > 0 ? "inf" : "-inf");
                return;
            }
            builder.append(number);
            return;
        }
        if (value instanceof Float) {
            Float number = (Float) value;
            builder.append(number);
            return;
        }
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            builder.append(decimal.toPlainString());
            return;
        }
        String text = String.valueOf(value);
        if (text.indexOf('\n') >= 0) {
            builder.append("\"\"\"\n").append(escape(text)).append("\"\"\"");
            return;
        }
        builder.append('"').append(Texts.escape(text)).append('"');
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean scalar(Items items) {
        for (Node element : items.elements()) {
            if (element instanceof Note) {
                continue;
            }
            if (!(element instanceof Value)) {
                return false;
            }
        }
        return true;
    }

    private String join(String path, String key) {
        String quoted = quote(key);
        if (path.isEmpty()) {
            return quoted;
        }
        return path + "." + quoted;
    }

    private String quote(String key) {
        if (key.isEmpty()) {
            return "\"\"";
        }
        for (int index = 0; index < key.length(); index++) {
            char current = key.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '_' || current == '-')) {
                return "\"" + Texts.escape(key) + "\"";
            }
        }
        return key;
    }

    private void writeComments(List<String> comments) {
        for (String comment : comments) {
            builder.append("# ").append(comment).append('\n');
        }
    }

    private void writeInline(String comment) {
        if (comment != null && !comment.isEmpty()) {
            builder.append(" # ").append(comment);
        }
    }
}
