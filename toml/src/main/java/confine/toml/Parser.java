package confine.toml;

import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

final class Parser {

    private final String text;
    private int index;
    private int line = 1;
    private final Block root = new Block();
    private Block current;
    private final List<String> pending = new ArrayList<>();

    Parser(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        this.text = text;
    }

    Block parse() {
        current = root;
        while (index < text.length()) {
            skipSpaces();
            if (index >= text.length()) {
                break;
            }
            char currentChar = text.charAt(index);
            if (currentChar == '\n' || currentChar == '\r') {
                newline();
                continue;
            }
            if (currentChar == '#') {
                pending.add(comment());
                continue;
            }
            if (currentChar == '[') {
                header();
                continue;
            }
            assignment();
        }
        for (String comment : pending) {
            current.add(new Note(comment));
        }
        pending.clear();
        return root;
    }

    private void header() {
        expect('[');
        boolean array = match('[');
        List<String> path = keys();
        expect(']');
        if (array) {
            expect(']');
        }
        if (path.isEmpty()) {
            throw error("empty toml table");
        }
        skipSpaces();
        String inline = null;
        if (index < text.length() && text.charAt(index) == '#') {
            inline = comment();
        }
        Block table = open(path, array);
        table.comments().addAll(pending);
        pending.clear();
        table.setInlineComment(inline);
        current = table;
    }

    private void assignment() {
        List<String> path = keys();
        if (path.isEmpty()) {
            throw error("expected toml key");
        }
        skipSpaces();
        expect('=');
        Node value = value();
        value.comments().addAll(pending);
        pending.clear();
        skipSpaces();
        if (index < text.length() && text.charAt(index) == '#') {
            value.setInlineComment(comment());
        }
        place(current, path, value);
    }

    private Block open(List<String> path, boolean array) {
        Block cursor = root;
        for (int part = 0; part < path.size() - 1; part++) {
            cursor = descend(cursor, path.get(part));
        }
        String leaf = path.get(path.size() - 1);
        if (!array) {
            Node existing = cursor.get(leaf);
            if (existing instanceof Block) {
                Block block = (Block) existing;
                return block;
            }
            if (existing != null) {
                throw error("toml path conflicts");
            }
            Block created = new Block();
            cursor.put(leaf, created);
            return created;
        }
        Items items;
        Node existing = cursor.get(leaf);
        if (existing == null) {
            items = new Items();
            cursor.put(leaf, items);
        } else if (existing instanceof Items) {
            Items found = (Items) existing;
            items = found;
        } else {
            throw error("toml path conflicts");
        }
        Block created = new Block();
        items.add(created);
        return created;
    }

    private Block descend(Block cursor, String key) {
        Node existing = cursor.get(key);
        if (existing == null) {
            Block created = new Block();
            cursor.put(key, created);
            return created;
        }
        if (existing instanceof Block) {
            Block block = (Block) existing;
            return block;
        }
        if (existing instanceof Items
                && ((Items) existing).size() > 0
                && ((Items) existing).get(((Items) existing).size() - 1) instanceof Block) {
            Items items = (Items) existing;
            Block last = (Block) items.get(items.size() - 1);
            return last;
        }
        throw error("toml path conflicts");
    }

    private void place(Block scope, List<String> path, Node value) {
        Block cursor = scope;
        for (int part = 0; part < path.size() - 1; part++) {
            Node existing = cursor.get(path.get(part));
            if (existing == null) {
                Block created = new Block();
                cursor.put(path.get(part), created);
                cursor = created;
                continue;
            }
            if (!(existing instanceof Block)) {
                throw error("toml path conflicts");
            }
            Block block = (Block) existing;
            cursor = block;
        }
        String leaf = path.get(path.size() - 1);
        if (cursor.has(leaf)) {
            throw error("duplicate toml key");
        }
        cursor.put(leaf, value);
    }

    private List<String> keys() {
        List<String> path = new ArrayList<>();
        while (index < text.length()) {
            skipSpaces();
            if (index >= text.length()) {
                break;
            }
            char currentChar = text.charAt(index);
            if (currentChar == '"' || currentChar == '\'') {
                Object parsed = string().value();
                path.add(String.valueOf(parsed));
            } else if (bare(currentChar)) {
                int start = index;
                while (index < text.length() && bare(text.charAt(index))) {
                    index++;
                }
                path.add(text.substring(start, index));
            } else {
                break;
            }
            skipSpaces();
            if (index < text.length() && text.charAt(index) == '.') {
                index++;
                continue;
            }
            break;
        }
        return path;
    }

    private Node value() {
        skipGap();
        if (index >= text.length()) {
            throw error("expected toml value");
        }
        char currentChar = text.charAt(index);
        if (currentChar == '"' || currentChar == '\'') {
            return string();
        }
        if (currentChar == '[') {
            return array();
        }
        if (currentChar == '{') {
            return inline();
        }
        String token = token();
        if (token.equals("true")) {
            return new Value(true);
        }
        if (token.equals("false")) {
            return new Value(false);
        }
        if (token.equals("inf") || token.equals("+inf")) {
            return new Value(Double.POSITIVE_INFINITY);
        }
        if (token.equals("-inf")) {
            return new Value(Double.NEGATIVE_INFINITY);
        }
        if (token.equals("nan") || token.equals("+nan") || token.equals("-nan")) {
            return new Value(Double.NaN);
        }
        if (date(token)) {
            return new Value(token);
        }
        Object number = number(token);
        if (number == null) {
            throw error("invalid toml value");
        }
        return new Value(number);
    }

    private Value string() {
        if (startsWith("\"\"\"")) {
            return new Value(multiline(true));
        }
        if (startsWith("'''")) {
            return new Value(multiline(false));
        }
        char quote = text.charAt(index);
        index++;
        if (quote == '\'') {
            StringBuilder builder = new StringBuilder();
            while (index < text.length() && text.charAt(index) != '\'') {
                char currentChar = text.charAt(index);
                if (currentChar == '\n') {
                    throw error("unterminated toml string");
                }
                builder.append(currentChar);
                index++;
            }
            expect('\'');
            return new Value(builder.toString());
        }
        return new Value(readBasic());
    }

    private String multiline(boolean basic) {
        index += 3;
        if (index < text.length() && text.charAt(index) == '\r') {
            index++;
        }
        if (index < text.length() && text.charAt(index) == '\n') {
            newline();
        }
        String closing = basic ? "\"\"\"" : "'''";
        StringBuilder builder = new StringBuilder();
        while (index < text.length() && !startsWith(closing)) {
            char currentChar = text.charAt(index);
            if (basic && currentChar == '\\') {
                if (index + 1 < text.length()
                        && (text.charAt(index + 1) == '\n' || text.charAt(index + 1) == '\r')) {
                    index++;
                    while (index < text.length()
                            && (text.charAt(index) == '\n'
                            || text.charAt(index) == '\r'
                            || text.charAt(index) == ' '
                            || text.charAt(index) == '\t')) {
                        if (text.charAt(index) == '\n') {
                            line++;
                        }
                        index++;
                    }
                    continue;
                }
                builder.append(escape());
                continue;
            }
            if (currentChar == '\n') {
                builder.append('\n');
                newline();
                continue;
            }
            builder.append(currentChar);
            index++;
        }
        if (!startsWith(closing)) {
            throw error("unterminated toml string");
        }
        index += 3;
        return builder.toString();
    }

    private String readBasic() {
        StringBuilder builder = new StringBuilder();
        while (index < text.length()) {
            char currentChar = text.charAt(index);
            if (currentChar == '"') {
                index++;
                return builder.toString();
            }
            if (currentChar == '\n') {
                throw error("unterminated toml string");
            }
            if (currentChar == '\\') {
                builder.append(escape());
                continue;
            }
            builder.append(currentChar);
            index++;
        }
        throw error("unterminated toml string");
    }

    private char escape() {
        index++;
        if (index >= text.length()) {
            throw error("invalid toml escape");
        }
        char currentChar = text.charAt(index++);
        switch (currentChar) {
                case 'b':
                    return '\b';
                case 't':
                    return '\t';
                case 'n':
                    return '\n';
                case 'f':
                    return '\f';
                case 'r':
                    return '\r';
                case '"':
                    return '"';
                case '\\':
                    return '\\';
                case 'u':
                    return unicode(4);
                case 'U':
                    return unicode(8);
                default:
                    throw error("invalid toml escape");
            }
    }

    private char unicode(int digits) {
        if (index + digits > text.length()) {
            throw error("invalid toml escape");
        }
        int value = Integer.parseInt(text.substring(index, index + digits), 16);
        index += digits;
        return (char) value;
    }

    private Items array() {
        expect('[');
        Items items = new Items();
        while (true) {
            skipGap();
            if (match(']')) {
                return items;
            }
            Node element = value();
            items.add(element);
            skipGap();
            if (match(']')) {
                return items;
            }
            expect(',');
        }
    }

    private Block inline() {
        expect('{');
        Block block = new Block();
        skipGap();
        if (match('}')) {
            return block;
        }
        while (index < text.length()) {
            List<String> path = keys();
            skipGap();
            expect('=');
            Node parsed = value();
            place(block, path, parsed);
            skipGap();
            if (match('}')) {
                return block;
            }
            expect(',');
            skipGap();
        }
        throw error("unterminated toml table");
    }

    private String token() {
        int start = index;
        while (index < text.length()) {
            char currentChar = text.charAt(index);
            if (Character.isWhitespace(currentChar)
                    || currentChar == ','
                    || currentChar == ']'
                    || currentChar == '}'
                    || currentChar == '#') {
                break;
            }
            index++;
        }
        if (start == index) {
            throw error("expected toml value");
        }
        return text.substring(start, index);
    }

    private Object number(String token) {
        String raw = token.replace("_", "");
        if (raw.length() > 2 && (raw.startsWith("0x")
                || raw.startsWith("0X")
                || raw.startsWith("+0x")
                || raw.startsWith("-0x")
                || raw.startsWith("+0X")
                || raw.startsWith("-0X"))) {
            return radix(raw, 16, 2);
        }
        if (raw.length() > 2 && (raw.startsWith("0o")
                || raw.startsWith("0O")
                || raw.startsWith("+0o")
                || raw.startsWith("-0o"))) {
            return radix(raw, 8, 2);
        }
        if (raw.length() > 2 && (raw.startsWith("0b")
                || raw.startsWith("0B")
                || raw.startsWith("+0b")
                || raw.startsWith("-0b"))) {
            return radix(raw, 2, 2);
        }
        int start = 0;
        if (raw.charAt(0) == '+' || raw.charAt(0) == '-') {
            start = 1;
        }
        boolean digit = false;
        boolean dot = false;
        boolean exp = false;
        for (int position = start; position < raw.length(); position++) {
            char currentChar = raw.charAt(position);
            if (currentChar >= '0' && currentChar <= '9') {
                digit = true;
                continue;
            }
            if (currentChar == '.' && !dot && !exp) {
                dot = true;
                continue;
            }
            if ((currentChar == 'e' || currentChar == 'E') && digit && !exp) {
                exp = true;
                if (position + 1 < raw.length() && (raw.charAt(position + 1) == '+' || raw.charAt(position + 1) == '-')) {
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
                return Double.valueOf(raw);
            }
            return fit(new BigInteger(raw));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object radix(String raw, int radix, int prefix) {
        int start = 0;
        boolean negative = false;
        if (raw.charAt(0) == '+' || raw.charAt(0) == '-') {
            negative = raw.charAt(0) == '-';
            start = 1;
        }
        try {
            BigInteger magnitude = new BigInteger(raw.substring(start + prefix), radix);
            return fit(negative ? magnitude.negate() : magnitude);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object fit(BigInteger number) {
        if (number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                && number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
            return number.intValue();
        }
        if (number.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
                && number.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
            return number.longValue();
        }
        return number;
    }

    private boolean date(String token) {
        if (token.length() >= 10 && token.charAt(4) == '-' && token.charAt(7) == '-') {
            return true;
        }
        return token.length() >= 8 && token.charAt(2) == ':' && token.charAt(5) == ':';
    }

    private void skipGap() {
        while (index < text.length()) {
            char currentChar = text.charAt(index);
            if (currentChar == ' ' || currentChar == '\t') {
                index++;
                continue;
            }
            if (currentChar == '\n' || currentChar == '\r') {
                newline();
                continue;
            }
            if (currentChar == '#') {
                pending.add(comment());
                continue;
            }
            return;
        }
    }

    private void skipSpaces() {
        while (index < text.length()) {
            char currentChar = text.charAt(index);
            if (currentChar != ' ' && currentChar != '\t') {
                return;
            }
            index++;
        }
    }

    private String comment() {
        expect('#');
        int start = index;
        while (index < text.length() && text.charAt(index) != '\n' && text.charAt(index) != '\r') {
            index++;
        }
        return text.substring(start, index).trim();
    }

    private void newline() {
        if (index < text.length() && text.charAt(index) == '\r') {
            index++;
        }
        if (index < text.length() && text.charAt(index) == '\n') {
            index++;
        }
        line++;
    }

    private boolean startsWith(String prefix) {
        return text.startsWith(prefix, index);
    }

    private boolean match(char currentChar) {
        if (index < text.length() && text.charAt(index) == currentChar) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(char currentChar) {
        if (index >= text.length() || text.charAt(index) != currentChar) {
            throw error("expected '" + currentChar + "'");
        }
        index++;
    }

    private boolean bare(char currentChar) {
        return Character.isLetterOrDigit(currentChar) || currentChar == '_' || currentChar == '-';
    }

    private IllegalStateException error(String message) {
        return new IllegalStateException(message + " at line " + line);
    }
}
