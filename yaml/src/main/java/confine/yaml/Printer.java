package confine.yaml;

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
        writeInline(root.inlineComment());
        if (root.inlineComment() != null) {
            builder.append('\n');
        }
        writeMapping(root, 0);
        return builder.toString();
    }

    private void writeMapping(Block block, int indent) {
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
            builder.append(Texts.spaces(indent));
            builder.append(quoteKey(child.name())).append(':');
            writeBody(child, indent);
        }
    }

    private void writeBody(Node node, int indent) {
        if (node instanceof Value) {
            Value value = (Value) node;
            if (value.value() instanceof String && ((String) value.value()).indexOf('\n') >= 0) {
                String text = (String) value.value();
                writeBlock(text, indent);
                writeInline(value.inlineComment());
                builder.append('\n');
                return;
            }
            builder.append(' ');
            writeScalar(value.value());
            writeInline(value.inlineComment());
            builder.append('\n');
            return;
        }
        if (node instanceof Block) {
            Block block = (Block) node;
            writeInline(block.inlineComment());
            builder.append('\n');
            writeMapping(block, indent + 2);
            return;
        }
        if (node instanceof Items) {
            Items items = (Items) node;
            writeInline(items.inlineComment());
            builder.append('\n');
            writeList(items, indent + 2);
            return;
        }
        throw new IllegalStateException("cannot write yaml node");
    }

    private void writeList(Items items, int indent) {
        for (Node element : items.elements()) {
            if (element instanceof Note) {
                Note note = (Note) element;
                writeComment(note.text(), indent);
                continue;
            }
            writeComments(element.comments(), indent);
            builder.append(Texts.spaces(indent)).append('-');
            if (element instanceof Block) {
                Block block = (Block) element;
                writeInline(block.inlineComment());
                builder.append('\n');
                writeMapping(block, indent + 2);
                continue;
            }
            if (element instanceof Items) {
                Items nested = (Items) element;
                writeInline(nested.inlineComment());
                builder.append('\n');
                writeList(nested, indent + 2);
                continue;
            }
            if (element instanceof Value) {
                Value value = (Value) element;
                if (value.value() instanceof String && ((String) value.value()).indexOf('\n') >= 0) {
                    String text = (String) value.value();
                    writeBlock(text, indent);
                } else {
                    builder.append(' ');
                    writeScalar(value.value());
                }
                writeInline(value.inlineComment());
                builder.append('\n');
                continue;
            }
            throw new IllegalStateException("cannot write yaml node");
        }
    }

    private void writeBlock(String text, int indent) {
        builder.append(" |\n");
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int start = 0;
        while (start <= normalized.length()) {
            int end = normalized.indexOf('\n', start);
            if (end < 0) {
                end = normalized.length();
            }
            builder.append(Texts.spaces(indent + 2));
            builder.append(normalized, start, end);
            builder.append('\n');
            if (end >= normalized.length()) {
                break;
            }
            start = end + 1;
        }
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
        if (value instanceof Double || value instanceof Float) {
            builder.append(value);
            return;
        }
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            builder.append(decimal.toPlainString());
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
        if (Character.isWhitespace(text.charAt(0))
                || Character.isWhitespace(text.charAt(text.length() - 1))) {
            return false;
        }
        if (text.equals("true")
                || text.equals("false")
                || text.equals("null")
                || text.equals("~")
                || text.equals("True")
                || text.equals("False")) {
            return false;
        }
        if (number(text)) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == ':'
                    || current == '#'
                    || current == '{'
                    || current == '}'
                    || current == '['
                    || current == ']'
                    || current == ','
                    || current == '&'
                    || current == '*'
                    || current == '!'
                    || current == '|'
                    || current == '>'
                    || current == '\''
                    || current == '"'
                    || current == '\\'
                    || current < 0x20) {
                return false;
            }
        }
        return true;
    }

    private boolean number(String text) {
        int start = 0;
        if (text.charAt(0) == '+' || text.charAt(0) == '-') {
            start = 1;
        }
        if (start >= text.length()) {
            return false;
        }
        boolean digit = false;
        boolean dot = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current >= '0' && current <= '9') {
                digit = true;
                continue;
            }
            if (current == '.' && !dot) {
                dot = true;
                continue;
            }
            return false;
        }
        return digit;
    }

    private String quoteKey(String key) {
        for (int index = 0; index < key.length(); index++) {
            char current = key.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '_' || current == '-')) {
                return "\"" + Texts.escape(key) + "\"";
            }
        }
        if (key.isEmpty()) {
            return "\"\"";
        }
        return key;
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
