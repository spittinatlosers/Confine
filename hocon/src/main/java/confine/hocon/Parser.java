package confine.hocon;

import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Parser {

    private final String text;
    private final int length;
    private int index;
    private int line = 1;
    private Block root;

    Parser(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        this.text = text;
        this.length = text.length();
    }

    Block parse() {
        List<String> leading = new ArrayList<>();
        skip(leading);
        Block root;
        if (index < length && text.charAt(index) == '{') {
            root = object(leading);
            skip(new ArrayList<>());
            if (index < length) {
                throw error("trailing hocon");
            }
        } else {
            root = new Block();
            root.comments().addAll(leading);
            body(root, '\0');
        }
        this.root = root;
        resolve(root);
        return root;
    }

    private void body(Block block, char closer) {
        while (index < length) {
            List<String> pending = new ArrayList<>();
            skip(pending);
            if (index >= length) {
                addNotes(block, pending);
                return;
            }
            char current = text.charAt(index);
            if (closer != '\0' && current == closer) {
                addNotes(block, pending);
                return;
            }
            if (current == ',' || current == ';') {
                index++;
                continue;
            }
            List<String> path = key();
            if (path.size() == 1 && path.get(0).equals("include")) {
                throw error("hocon include is not supported");
            }
            skip(pending);
            if (index < length && text.charAt(index) == '+') {
                throw error("unsupported hocon operator");
            }
            if (index < length && (text.charAt(index) == '=' || text.charAt(index) == ':')) {
                if (text.charAt(index) == '='
                        && index + 1 < length
                        && text.charAt(index + 1) == '=') {
                    throw error("unsupported hocon operator");
                }
                if (text.charAt(index) == '+' ) {
                    throw error("unsupported hocon operator");
                }
                index++;
                if (index < length && text.charAt(index) == '=') {
                    throw error("unsupported hocon operator");
                }
            }
            Node value = value();
            value.comments().addAll(pending);
            place(block, path, value);
        }
    }

    private Node value() {
        List<String> pending = new ArrayList<>();
        skip(pending);
        if (index >= length) {
            throw error("expected hocon value");
        }
        char current = text.charAt(index);
        Node node;
        if (current == '{') {
            node = object(pending);
        } else if (current == '[') {
            node = array(pending);
        } else if (current == '"' || current == '\'') {
            node = new Value(quoted(current));
            node.comments().addAll(pending);
        } else {
            node = new Value(classify(raw()));
            node.comments().addAll(pending);
        }
        skipInline(node);
        return node;
    }

    private Block object(List<String> pending) {
        expect('{');
        Block block = new Block();
        block.comments().addAll(pending);
        body(block, '}');
        expect('}');
        return block;
    }

    private Items array(List<String> pending) {
        expect('[');
        Items items = new Items();
        items.comments().addAll(pending);
        while (index < length) {
            List<String> itemPending = new ArrayList<>();
            skip(itemPending);
            if (index < length && text.charAt(index) == ']') {
                break;
            }
            if (index < length && (text.charAt(index) == ',' || text.charAt(index) == ';')) {
                index++;
                continue;
            }
            Node element = value();
            element.comments().addAll(itemPending);
            items.add(element);
        }
        expect(']');
        return items;
    }

    private List<String> key() {
        List<String> path = new ArrayList<>();
        while (index < length) {
            char current = text.charAt(index);
            if (current == '"' || current == '\'') {
                path.add(quoted(current));
            } else {
                int start = index;
                while (index < length) {
                    char next = text.charAt(index);
                    if (next == '.'
                            || next == '='
                            || next == ':'
                            || next == '{'
                            || next == '}'
                            || next == '['
                            || next == ']'
                            || next == ','
                            || next == '#'
                            || next == '\n'
                            || next == '\r'
                            || next == ' '
                            || next == '\t') {
                        break;
                    }
                    if (next == '/' && index + 1 < length && text.charAt(index + 1) == '/') {
                        break;
                    }
                    index++;
                }
                if (start == index) {
                    throw error("expected hocon key");
                }
                path.add(text.substring(start, index).trim());
            }
            int mark = index;
            while (index < length && (text.charAt(index) == ' ' || text.charAt(index) == '\t')) {
                index++;
            }
            if (index < length && text.charAt(index) == '.') {
                index++;
                while (index < length && (text.charAt(index) == ' ' || text.charAt(index) == '\t')) {
                    index++;
                }
                continue;
            }
            index = mark;
            break;
        }
        if (path.isEmpty()) {
            throw error("expected hocon key");
        }
        return path;
    }

    private String raw() {
        int start = index;
        int substitution = 0;
        while (index < length) {
            char current = text.charAt(index);
            if (current == '$' && index + 1 < length && text.charAt(index + 1) == '{') {
                substitution++;
                index += 2;
                continue;
            }
            if (current == '}' && substitution > 0) {
                substitution--;
                index++;
                continue;
            }
            if (substitution == 0 && (current == '\n'
                    || current == '\r'
                    || current == ','
                    || current == '}'
                    || current == ']'
                    || current == '#'
                    || current == ';')) {
                break;
            }
            if (substitution == 0
                    && current == '/'
                    && index + 1 < length
                    && text.charAt(index + 1) == '/'
                    && (index == start || Character.isWhitespace(text.charAt(index - 1)))) {
                break;
            }
            index++;
        }
        return text.substring(start, index).trim();
    }

    private Object classify(String token) {
        if (token.isEmpty()) {
            throw error("expected hocon value");
        }
        if (token.contains("${")) {
            return token;
        }
        if (token.equals("true")) {
            return true;
        }
        if (token.equals("false")) {
            return false;
        }
        if (token.equals("null")) {
            return null;
        }
        Object number = number(token);
        if (number != null) {
            return number;
        }
        return token;
    }

    private String quoted(char quote) {
        expect(quote);
        StringBuilder builder = new StringBuilder();
        while (index < length) {
            char current = text.charAt(index);
            if (current == quote) {
                index++;
                return builder.toString();
            }
            if (current == '\\') {
                builder.append(escape());
                continue;
            }
            if (current == '\n') {
                line++;
            }
            builder.append(current);
            index++;
        }
        throw error("unterminated hocon string");
    }

    private char escape() {
        index++;
        if (index >= length) {
            throw error("invalid hocon escape");
        }
        char current = text.charAt(index++);
        switch (current) {
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case '"':
                    return '"';
                case '\'':
                    return '\'';
                case '\\':
                    return '\\';
                case '/':
                    return '/';
                case 'u':
                if (index + 4 > length) {
                    throw error("invalid hocon escape");
                }
                char value = (char) Integer.parseInt(text.substring(index, index + 4), 16);
                index += 4;
                return value;

                default:
                    return current;
            }
    }

    private Object number(String token) {
        int start = 0;
        if (token.charAt(0) == '+' || token.charAt(0) == '-') {
            start = 1;
        }
        if (start >= token.length()) {
            return null;
        }
        boolean digit = false;
        boolean dot = false;
        boolean exp = false;
        for (int position = start; position < token.length(); position++) {
            char current = token.charAt(position);
            if (current >= '0' && current <= '9') {
                digit = true;
                continue;
            }
            if (current == '.' && !dot && !exp) {
                dot = true;
                continue;
            }
            if ((current == 'e' || current == 'E') && digit && !exp) {
                exp = true;
                if (position + 1 < token.length() && (token.charAt(position + 1) == '+' || token.charAt(position + 1) == '-')) {
                    position++;
                }
                continue;
            }
            return null;
        }
        if (!digit) {
            return null;
        }
        try {
            if (dot || exp) {
                return Double.valueOf(token);
            }
            BigInteger number = new BigInteger(token);
            if (number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                    && number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
                return number.intValue();
            }
            if (number.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
                    && number.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
                return number.longValue();
            }
            return number;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void resolve(Block root) {
        walk(root, new HashSet<>());
    }

    private void walk(Node node, Set<String> stack) {
        if (node instanceof Block) {
            Block block = (Block) node;
            for (Node child : Collections.unmodifiableList(new ArrayList<>(block.ordered()))) {
                walk(child, stack);
            }
            return;
        }
        if (node instanceof Items) {
            Items items = (Items) node;
            for (int position = 0; position < items.size(); position++) {
                walk(items.get(position), stack);
            }
            return;
        }
        if (!(node instanceof Value)) {
            return;
        }
        Value value = (Value) node;
        if (!(value.value() instanceof String)) {
            return;
        }
        String token = (String) value.value();
        if (!token.contains("${")) {
            return;
        }
        Object resolved = resolveText(token, stack);
        replace(value, resolved);
    }

    private Object resolveText(String token, Set<String> stack) {
        String trimmed = token.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}") && trimmed.indexOf("${", 2) < 0) {
            return lookup(trimmed, stack);
        }
        StringBuilder builder = new StringBuilder();
        int position = 0;
        while (position < token.length()) {
            int open = token.indexOf("${", position);
            if (open < 0) {
                builder.append(token.substring(position));
                break;
            }
            builder.append(token, position, open);
            int close = token.indexOf('}', open);
            if (close < 0) {
                throw error("unterminated hocon substitution");
            }
            Object found = lookup(token.substring(open, close + 1), stack);
            if (found != null) {
                builder.append(found);
            }
            position = close + 1;
        }
        return builder.toString();
    }

    private Object lookup(String expression, Set<String> stack) {
        boolean optional = expression.startsWith("${?");
        String path = expression.substring(optional ? 3 : 2, expression.length() - 1).trim();
        if (path.isEmpty()) {
            throw error("empty hocon substitution");
        }
        if (!stack.add(path)) {
            throw error("cyclic hocon substitution " + path);
        }
        try {
            Node node = find(path);
            if (node == null) {
                if (optional) {
                    return null;
                }
                throw error("missing hocon substitution " + path);
            }
            if (node instanceof Value) {
                Value value = (Value) node;
                Object raw = value.value();
                if (raw instanceof String && ((String) raw).contains("${")) {
                    String token = (String) raw;
                    Object resolved = resolveText(token, stack);
                    replace(value, resolved);
                    return resolved;
                }
                return raw;
            }
            return null;
        } finally {
            stack.remove(path);
        }
    }

    private Node find(String path) {
        Node current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Block)) {
                return null;
            }
            Block block = (Block) current;
            current = block.get(part.trim());
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void replace(Value value, Object resolved) {
        Node parent = value.parent();
        Value updated = new Value(resolved);
        updated.comments().addAll(value.comments());
        updated.setInlineComment(value.inlineComment());
        if (parent instanceof Block) {
            Block block = (Block) parent;
            block.put(value.name(), updated);
            return;
        }
        if (parent instanceof Items) {
            Items items = (Items) parent;
            for (int position = 0; position < items.size(); position++) {
                if (items.get(position) == value) {
                    items.set(position, updated);
                    return;
                }
            }
        }
    }

    private void place(Block block, List<String> path, Node value) {
        Block cursor = block;
        for (int part = 0; part < path.size() - 1; part++) {
            Node existing = cursor.get(path.get(part));
            if (existing == null) {
                Block created = new Block();
                cursor.put(path.get(part), created);
                cursor = created;
                continue;
            }
            if (!(existing instanceof Block)) {
                throw error("hocon path conflicts");
            }
            Block nested = (Block) existing;
            cursor = nested;
        }
        cursor.put(path.get(path.size() - 1), value);
    }

    private void addNotes(Block block, List<String> pending) {
        for (String comment : pending) {
            block.add(new Note(comment));
        }
    }

    private void skip(List<String> pending) {
        while (index < length) {
            char current = text.charAt(index);
            if (current == ' ' || current == '\t' || current == '\f') {
                index++;
                continue;
            }
            if (current == '\r') {
                index++;
                continue;
            }
            if (current == '\n') {
                index++;
                line++;
                continue;
            }
            if (current == '#') {
                pending.add(comment());
                continue;
            }
            if (current == '/' && index + 1 < length && text.charAt(index + 1) == '/') {
                index += 2;
                pending.add(commentRest());
                continue;
            }
            return;
        }
    }

    private void skipInline(Node node) {
        int mark = index;
        while (index < length && (text.charAt(index) == ' ' || text.charAt(index) == '\t')) {
            index++;
        }
        if (index < length && text.charAt(index) == '#') {
            node.setInlineComment(comment());
            return;
        }
        if (index + 1 < length && text.charAt(index) == '/' && text.charAt(index + 1) == '/') {
            index += 2;
            node.setInlineComment(commentRest());
            return;
        }
        index = mark;
    }

    private String comment() {
        index++;
        return commentRest();
    }

    private String commentRest() {
        int start = index;
        while (index < length && text.charAt(index) != '\n' && text.charAt(index) != '\r') {
            index++;
        }
        return text.substring(start, index).trim();
    }

    private void expect(char current) {
        if (index >= length || text.charAt(index) != current) {
            throw error("expected '" + current + "'");
        }
        index++;
    }

    private IllegalStateException error(String message) {
        return new IllegalStateException(message + " at line " + line);
    }
}
