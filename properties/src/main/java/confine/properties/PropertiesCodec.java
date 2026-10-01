package confine.properties;

import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PropertiesCodec {

    Block parse(String text) {
        Block root = new Block();
        List<String> pending = new ArrayList<>();
        String logical = joinLines(text);
        int cursor = 0;
        while (cursor <= logical.length()) {
            int end = logical.indexOf('\n', cursor);
            if (end < 0) {
                end = logical.length();
            }
            String line = logical.substring(cursor, end);
            cursor = end + 1;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.charAt(0) == '#' || trimmed.charAt(0) == '!') {
                pending.add(trimmed.substring(1).trim());
                continue;
            }
            int split = separator(line);
            String keyRaw;
            String valueRaw;
            if (split < 0) {
                keyRaw = line.trim();
                valueRaw = "";
            } else {
                keyRaw = line.substring(0, split).trim();
                valueRaw = line.substring(split + 1);
                int valueStart = 0;
                while (valueStart < valueRaw.length() && Character.isWhitespace(valueRaw.charAt(valueStart))) {
                    valueStart++;
                }
                valueRaw = valueRaw.substring(valueStart);
            }
            Object scalar = parseScalar(valueRaw);
            Node node = new Value(scalar);
            node.comments().addAll(pending);
            pending.clear();
            insert(root, keyRaw, node);
            if (cursor > logical.length()) {
                break;
            }
        }
        for (String comment : pending) {
            root.add(new Note(comment));
        }
        return root;
    }

    String render(Block root) {
        StringBuilder builder = new StringBuilder();
        for (String comment : root.comments()) {
            builder.append("# ").append(comment).append('\n');
        }
        writeSection(root, "", builder);
        for (Node child : root.ordered()) {
            if (child instanceof Note) {
                Note commentNode = (Note) child;
                builder.append("# ").append(commentNode.text()).append('\n');
            }
        }
        return builder.toString();
    }

    private void writeSection(Block section, String prefix, StringBuilder builder) {
        if (!prefix.isEmpty()) {
            for (String comment : section.comments()) {
                builder.append("# ").append(comment).append('\n');
            }
        }
        for (Node child : section.ordered()) {
            if (child instanceof Note || child.name() == null) {
                continue;
            }
            String path = prefix.isEmpty() ? escapeKey(child.name()) : prefix
                    + "."
                    + escapeKey(child.name());
            if (child instanceof Block) {
                Block nested = (Block) child;
                writeSection(nested, path, builder);
                continue;
            }
            if (child instanceof Items) {
                Items listNode = (Items) child;
                writeList(listNode, path, builder);
                continue;
            }
            if (child instanceof Value) {
                Value valueNode = (Value) child;
                for (String comment : child.comments()) {
                    builder.append("# ").append(comment).append('\n');
                }
                builder
                        .append(path)
                        .append('=')
                        .append(renderScalar(valueNode.value()))
                        .append('\n');
            }
        }
    }

    private void writeList(Items list, String prefix, StringBuilder builder) {
        for (String comment : list.comments()) {
            builder.append("# ").append(comment).append('\n');
        }
        int index = 0;
        for (Node element : list.elements()) {
            if (element instanceof Note) {
                continue;
            }
            String path = prefix + "[" + index + "]";
            if (element instanceof Block) {
                Block sectionNode = (Block) element;
                writeSection(sectionNode, path, builder);
            } else if (element instanceof Items) {
                Items nested = (Items) element;
                writeList(nested, path, builder);
            } else if (element instanceof Value) {
                Value valueNode = (Value) element;
                for (String comment : element.comments()) {
                    builder.append("# ").append(comment).append('\n');
                }
                builder
                        .append(path)
                        .append('=')
                        .append(renderScalar(valueNode.value()))
                        .append('\n');
            }
            index++;
        }
    }

    private void insert(Block root, String key, Node value) {
        List<Token> tokens = tokenize(key);
        if (tokens.isEmpty()) {
            throw new IllegalStateException("empty properties key");
        }
        Node current = root;
        for (int index = 0; index < tokens.size() - 1; index++) {
            current = descend(current, tokens.get(index), containerFor(tokens.get(index + 1)));
        }
        Token leaf = tokens.get(tokens.size() - 1);
        if (leaf.index()) {
            Items list = asList(current);
            ensureSize(list, leaf.position() + 1);
            list.set(leaf.position(), value);
            return;
        }
        if (!(current instanceof Block)) {
            throw new IllegalStateException("properties path conflicts at " + key);
        }
        Block sectionNode = (Block) current;
        sectionNode.put(leaf.text(), value);
    }

    private Node descend(Node current, Token token, NodeKindNeeded needed) {
        if (token.index()) {
            Items list = asList(current);
            ensureSize(list, token.position() + 1);
            Node existing = list.get(token.position());
            if (existing == null || existing instanceof Value && ((Value) existing).isNull()) {
                Node created = needed == NodeKindNeeded.LIST ? new Items() : new Block();
                list.set(token.position(), created);
                return created;
            }
            if (needed == NodeKindNeeded.LIST && !(existing instanceof Items)) {
                throw new IllegalStateException("properties path conflicts");
            }
            if (needed == NodeKindNeeded.SECTION && !(existing instanceof Block)) {
                throw new IllegalStateException("properties path conflicts");
            }
            return existing;
        }
        if (!(current instanceof Block)) {
            throw new IllegalStateException("properties path conflicts");
        }
        Block sectionNode = (Block) current;
        Node existing = sectionNode.get(token.text());
        if (existing == null) {
            Node created = needed == NodeKindNeeded.LIST ? new Items() : new Block();
            sectionNode.put(token.text(), created);
            return created;
        }
        if (needed == NodeKindNeeded.LIST && !(existing instanceof Items)) {
            throw new IllegalStateException("properties path conflicts at " + token.text());
        }
        if (needed == NodeKindNeeded.SECTION && !(existing instanceof Block)) {
            throw new IllegalStateException("properties path conflicts at " + token.text());
        }
        return existing;
    }

    private NodeKindNeeded containerFor(Token next) {
        if (next.index()) {
            return NodeKindNeeded.LIST;
        }
        return NodeKindNeeded.SECTION;
    }

    private Items asList(Node current) {
        if (current instanceof Items) {
            Items listNode = (Items) current;
            return listNode;
        }
        throw new IllegalStateException("properties path conflicts");
    }

    private void ensureSize(Items list, int size) {
        while (list.size() < size) {
            list.add(new Value(null));
        }
    }

    private List<Token> tokenize(String key) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        for (int index = 0; index < key.length(); index++) {
            char current = key.charAt(index);
            if (current == '\\' && index + 1 < key.length()) {
                name.append(key.charAt(index + 1));
                index++;
                continue;
            }
            if (current == '.') {
                if (name.length() == 0) {
                    if (tokens.isEmpty() || !tokens.get(tokens.size() - 1).index()) {
                        throw new IllegalStateException("empty properties segment");
                    }
                    continue;
                }
                tokens.add(Token.key(unescape(name.toString())));
                name.setLength(0);
                continue;
            }
            if (current == '[') {
                if (name.length() != 0) {
                    tokens.add(Token.key(name.toString()));
                    name.setLength(0);
                }
                int close = key.indexOf(']', index);
                if (close < 0) {
                    throw new IllegalStateException("unterminated index");
                }
                String raw = key.substring(index + 1, close).trim();
                if (raw.isEmpty() || !digits(raw)) {
                    throw new IllegalStateException("invalid index " + raw);
                }
                tokens.add(Token.index(Integer.parseInt(raw)));
                index = close;
                continue;
            }
            name.append(current);
        }
        if (name.length() != 0) {
            tokens.add(Token.key(unescape(name.toString())));
        }
        return tokens;
    }

    private boolean digits(String raw) {
        for (int index = 0; index < raw.length(); index++) {
            if (!Character.isDigit(raw.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private int separator(String line) {
        boolean escape = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (escape) {
                escape = false;
                continue;
            }
            if (current == '\\') {
                escape = true;
                continue;
            }
            if (current == '=' || current == ':') {
                return index;
            }
        }
        return -1;
    }

    private String joinLines(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder builder = new StringBuilder();
        boolean escape = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (escape) {
                if (current == '\n') {
                    escape = false;
                    continue;
                }
                builder.append('\\');
                builder.append(current);
                escape = false;
                continue;
            }
            if (current == '\\') {
                escape = true;
                continue;
            }
            builder.append(current);
        }
        if (escape) {
            builder.append('\\');
        }
        return builder.toString();
    }

    private Object parseScalar(String raw) {
        if (raw.length() >= 2 && raw.charAt(0) == '"' && raw.charAt(raw.length() - 1) == '"') {
            return unescape(raw.substring(1, raw.length() - 1));
        }
        String trimmed = raw.trim();
        if (trimmed.equals("null")) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("true")) {
            return true;
        }
        if (lower.equals("false")) {
            return false;
        }
        if (isInteger(trimmed)) {
            try {
                long number = Long.parseLong(trimmed);
                if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                    return (int) number;
                }
                return number;
            } catch (NumberFormatException exception) {
                return unescape(raw);
            }
        }
        if (isDecimal(trimmed)) {
            try {
                return new BigDecimal(trimmed);
            } catch (NumberFormatException exception) {
                return unescape(raw);
            }
        }
        return unescape(raw);
    }

    private String renderScalar(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return String.valueOf(value);
        }
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            return decimal.toPlainString();
        }
        if (value instanceof Float || value instanceof Double) {
            return String.valueOf(value);
        }
        String text = String.valueOf(value);
        if (mustQuote(text)) {
            return "\"" + escapeValue(text) + "\"";
        }
        return escapeValue(text);
    }

    private boolean mustQuote(String text) {
        if (text.isEmpty()) {
            return true;
        }
        String trimmed = text.trim();
        if (!trimmed.equals(text)) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("false") || lower.equals("null")) {
            return true;
        }
        if (isInteger(trimmed) || isDecimal(trimmed)) {
            return true;
        }
        return text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0
                || text.indexOf('\\') >= 0
                || text.indexOf('"') >= 0;
    }

    private boolean isInteger(String text) {
        if (text.isEmpty()) {
            return false;
        }
        int start = text.charAt(0) == '-' || text.charAt(0) == '+' ? 1 : 0;
        if (start == text.length()) {
            return false;
        }
        for (int index = start; index < text.length(); index++) {
            if (!Character.isDigit(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean isDecimal(String text) {
        if (text.isEmpty()) {
            return false;
        }
        boolean digit = false;
        boolean dot = false;
        int start = 0;
        if (text.charAt(0) == '-' || text.charAt(0) == '+') {
            start = 1;
        }
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (Character.isDigit(current)) {
                digit = true;
                continue;
            }
            if (current == '.' && !dot) {
                dot = true;
                continue;
            }
            if (current == 'e' || current == 'E') {
                return digit && index + 1 < text.length();
            }
            return false;
        }
        return digit && dot;
    }

    private String escapeKey(String key) {
        return key
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("=", "\\=")
                .replace(":", "\\:")
                .replace("[", "\\[")
                .replace(" ", "\\ ");
    }

    private String escapeValue(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(current);
                    break;
            }
        }
        return builder.toString();
    }

    private String unescape(String raw) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current != '\\' || index + 1 >= raw.length()) {
                builder.append(current);
                continue;
            }
            char escaped = raw.charAt(++index);
            switch (escaped) {
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    if (index + 4 >= raw.length()) {
                        throw new IllegalStateException("invalid unicode escape");
                    }
                    String hex = raw.substring(index + 1, index + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    index += 4;
                    break;
                default:
                    builder.append(escaped);
                    break;
            }
        }
        return builder.toString();
    }

    private enum NodeKindNeeded {
        SECTION,
        LIST
    }

    private static final class Token {

        private final String text;
        private final int position;
        private final boolean index;

        private Token(String text, int position, boolean index) {
            this.text = text;
            this.position = position;
            this.index = index;
        }

        static Token key(String text) {
            return new Token(text, -1, false);
        }

        static Token index(int position) {
            return new Token(null, position, true);
        }

        String text() {
            return text;
        }

        int position() {
            return position;
        }

        boolean index() {
            return index;
        }
    }
}
