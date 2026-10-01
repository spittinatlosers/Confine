package confine.json;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class Json5Parser {

    private final String text;
    private final int length;
    private final boolean strict;
    private int index;
    private int line = 1;
    private int column = 1;

    Json5Parser(String text, boolean strict) {
        this.text = text;
        this.length = text.length();
        this.strict = strict;
    }

    Block parse() {
        List<String> leading = skipTrivia();
        if (eof()) {
            Block empty = new Block();
            empty.comments().addAll(leading);
            return empty;
        }
        if (peek() != '{') {
            throw error("configuration root must be an object");
        }
        Block section = parseObject();
        section.comments().addAll(0, leading);
        List<String> trailing = skipTrivia();
        for (String comment : trailing) {
            section.add(new Note(comment));
        }
        if (!eof()) {
            throw error("trailing data");
        }
        return section;
    }

    private Block parseObject() {
        expect('{');
        Block section = new Block();
        boolean expectComma = false;
        while (!eof()) {
            List<String> leading = skipTrivia();
            if (eof()) {
                throw error("unterminated object");
            }
            if (peek() == '}') {
                if (expectComma && !leading.isEmpty() && strict) {
                    throw error("trailing comma");
                }
                consume();
                return section;
            }
            if (expectComma) {
                if (peek() != ',') {
                    throw error("expected comma");
                }
                consume();
                leading.addAll(skipTrivia());
                if (peek() == '}') {
                    if (strict) {
                        throw error("trailing comma");
                    }
                    consume();
                    return section;
                }
            }
            String inlineBefore = null;
            String key = parseKey();
            skipSpaces();
            if (!strict) {
                inlineBefore = readInline();
            }
            expect(':');
            Node value = parseValue();
            if (!leading.isEmpty()) {
                List<String> combined = new ArrayList<>(leading);
                combined.addAll(value.comments());
                value.comments().clear();
                value.comments().addAll(combined);
            }
            if (inlineBefore != null && (value.inlineComment() == null || Texts.blank(value.inlineComment()))) {
                value.setInlineComment(inlineBefore);
            }
            readInlineInto(value);
            section.put(key, value);
            expectComma = true;
        }
        throw error("unterminated object");
    }

    private Items parseArray() {
        expect('[');
        Items list = new Items();
        boolean expectComma = false;
        while (!eof()) {
            List<String> leading = skipTrivia();
            if (eof()) {
                throw error("unterminated array");
            }
            if (peek() == ']') {
                consume();
                return list;
            }
            if (expectComma) {
                if (peek() != ',') {
                    throw error("expected comma");
                }
                consume();
                leading.addAll(skipTrivia());
                if (peek() == ']') {
                    if (strict) {
                        throw error("trailing comma");
                    }
                    consume();
                    return list;
                }
            }
            Node value = parseValue();
            if (!leading.isEmpty()) {
                List<String> combined = new ArrayList<>(leading);
                combined.addAll(value.comments());
                value.comments().clear();
                value.comments().addAll(combined);
            }
            readInlineInto(value);
            list.add(value);
            expectComma = true;
        }
        throw error("unterminated array");
    }

    private Node parseValue() {
        List<String> leading = skipTrivia();
        Node node = parseBare();
        if (!leading.isEmpty()) {
            node.comments().addAll(0, leading);
        }
        readInlineInto(node);
        return node;
    }

    private Node parseBare() {
        if (eof()) {
            throw error("expected value");
        }
        char current = peek();
        if (current == '{') {
            return parseObject();
        }
        if (current == '[') {
            return parseArray();
        }
        if (current == '"' || current == '\'') {
            return new Value(parseString());
        }
        if (current == '+' || current == '-' || current == '.' || Character.isDigit(current)) {
            return new Value(parseNumber());
        }
        String literal = readIdentifier();
        if (literal.equals("true")) {
            return new Value(true);
        }
        if (literal.equals("false")) {
            return new Value(false);
        }
        if (literal.equals("null")) {
            return new Value(null);
        }
        if (!strict && (literal.equals("Infinity") || literal.equals("NaN"))) {
            return new Value(literal.equals("NaN") ? Double.NaN : Double.POSITIVE_INFINITY);
        }
        throw error("unexpected token " + literal);
    }

    private String parseKey() {
        skipTrivia();
        char current = peek();
        if (current == '"' || current == '\'') {
            return parseString();
        }
        if (strict) {
            throw error("strict json requires quoted keys");
        }
        String identifier = readIdentifier();
        if (identifier.isEmpty()) {
            throw error("expected key");
        }
        return identifier;
    }

    private String parseString() {
        char quote = peek();
        if (strict && quote == '\'') {
            throw error("strict json rejects single quotes");
        }
        consume();
        StringBuilder builder = new StringBuilder();
        while (!eof()) {
            char current = peek();
            if (current == quote) {
                consume();
                return builder.toString();
            }
            if (current == '\\') {
                consume();
                if (eof()) {
                    throw error("unterminated escape");
                }
                char escaped = consume();
                switch (escaped) {
                case '"':
                case '\'':
                case '\\':
                case '/':
                    builder.append(escaped);
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
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
                    builder.append(hex(4));
                    break;
                case 'x':
                    if (strict) {
                        throw error("strict json rejects hex escapes");
                    }
                    builder.append(hex(2));
                    break;
                case '\n':
                    if (strict) {
                        throw error("strict json rejects line continuation");
                    }
                    break;
                case '\r':
                    if (strict) {
                        throw error("strict json rejects line continuation");
                    }
                    if (!eof() && peek() == '\n') {
                        consume();
                    }
                    break;
                default:
                    throw error("invalid escape");
            }
            continue;
            }
            if (current == '\n' || current == '\r') {
                throw error("unterminated string");
            }
            builder.append(consume());
        }
        throw error("unterminated string");
    }

    private Object parseNumber() {
        int start = index;
        char first = peek();
        if (first == '+') {
            if (strict) {
                throw error("strict json rejects leading plus");
            }
            consume();
        } else if (first == '-') {
            consume();
        }
        if (!eof() && (matchWord("Infinity") || matchWord("NaN"))) {
            if (strict) {
                throw error("strict json rejects non-finite numbers");
            }
            boolean negative = text.charAt(start) == '-';
            if (text.startsWith("NaN", index - 3)) {
                return Double.NaN;
            }
            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        if (!eof()
                && peek() == '0'
                && index + 1 < length
                && (text.charAt(index + 1) == 'x' || text.charAt(index + 1) == 'X')) {
            if (strict) {
                throw error("strict json rejects hexadecimal numbers");
            }
            consume();
            consume();
            int hexStart = index;
            while (!eof() && isHex(peek())) {
                consume();
            }
            if (hexStart == index) {
                throw error("expected hexadecimal digit");
            }
            String digits = text.substring(hexStart, index);
            long magnitude = Long.parseUnsignedLong(digits, 16);
            long signed = text.charAt(start) == '-' ? -magnitude : magnitude;
            if (signed >= Integer.MIN_VALUE && signed <= Integer.MAX_VALUE) {
                return (int) signed;
            }
            return signed;
        }
        if (!eof() && peek() == '.') {
            if (strict) {
                throw error("strict json rejects a leading decimal point");
            }
        }
        boolean digit = false;
        while (!eof() && Character.isDigit(peek())) {
            digit = true;
            consume();
        }
        boolean decimal = false;
        if (!eof() && peek() == '.') {
            decimal = true;
            consume();
            while (!eof() && Character.isDigit(peek())) {
                digit = true;
                consume();
            }
        }
        if (!digit) {
            throw error("expected digit");
        }
        if (!eof() && (peek() == 'e' || peek() == 'E')) {
            decimal = true;
            consume();
            if (!eof() && (peek() == '+' || peek() == '-')) {
                consume();
            }
            int exponentStart = index;
            while (!eof() && Character.isDigit(peek())) {
                consume();
            }
            if (exponentStart == index) {
                throw error("expected exponent");
            }
        }
        String raw = text.substring(start, index);
        if (!decimal && !raw.contains(".") && !raw.contains("e") && !raw.contains("E")) {
            try {
                long number = Long.parseLong(raw);
                if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                    return (int) number;
                }
                return number;
            } catch (NumberFormatException exception) {
                return new BigDecimal(raw);
            }
        }
        return new BigDecimal(raw);
    }

    private String readIdentifier() {
        if (eof()) {
            return "";
        }
        char first = peek();
        if (!(Character.isLetter(first) || first == '_' || first == '$')) {
            if (first == '+' || first == '-') {
                return "";
            }
            return "";
        }
        StringBuilder builder = new StringBuilder();
        while (!eof()) {
            char current = peek();
            if (Character.isLetterOrDigit(current) || current == '_' || current == '$') {
                builder.append(consume());
                continue;
            }
            break;
        }
        return builder.toString();
    }

    private List<String> skipTrivia() {
        List<String> comments = new ArrayList<>();
        while (!eof()) {
            char current = peek();
            if (current == ' ' || current == '\t' || current == '\n' || current == '\r') {
                consume();
                continue;
            }
            if (current == '/' && index + 1 < length) {
                char next = text.charAt(index + 1);
                if (next == '/') {
                    if (strict) {
                        throw error("strict json rejects comments");
                    }
                    consume();
                    consume();
                    comments.add(readUntilLine());
                    continue;
                }
                if (next == '*') {
                    if (strict) {
                        throw error("strict json rejects comments");
                    }
                    consume();
                    consume();
                    comments.add(readBlock());
                    continue;
                }
            }
            break;
        }
        return comments;
    }

    private void skipSpaces() {
        while (!eof()) {
            char current = peek();
            if (current == ' ' || current == '\t') {
                consume();
                continue;
            }
            break;
        }
    }

    private void readInlineInto(Node node) {
        int mark = index;
        int markLine = line;
        int markColumn = column;
        skipSpaces();
        String inline = readInline();
        if (inline == null) {
            index = mark;
            line = markLine;
            column = markColumn;
            return;
        }
        if (node.inlineComment() == null || Texts.blank(node.inlineComment())) {
            node.setInlineComment(inline);
        }
    }

    private String readInline() {
        if (strict || eof() || peek() != '/') {
            return null;
        }
        if (index + 1 >= length) {
            return null;
        }
        char next = text.charAt(index + 1);
        if (next == '/') {
            consume();
            consume();
            return readUntilLine().trim();
        }
        if (next == '*') {
            int mark = index;
            int markLine = line;
            int markColumn = column;
            consume();
            consume();
            String comment = readBlock();
            if (comment.contains("\n")) {
                index = mark;
                line = markLine;
                column = markColumn;
                return null;
            }
            return comment.trim();
        }
        return null;
    }

    private String readUntilLine() {
        StringBuilder builder = new StringBuilder();
        while (!eof() && peek() != '\n' && peek() != '\r') {
            builder.append(consume());
        }
        return builder.toString().trim();
    }

    private String readBlock() {
        StringBuilder builder = new StringBuilder();
        while (!eof()) {
            if (peek() == '*' && index + 1 < length && text.charAt(index + 1) == '/') {
                consume();
                consume();
                return builder.toString().trim();
            }
            builder.append(consume());
        }
        throw error("unterminated comment");
    }

    private char hex(int count) {
        int value = 0;
        for (int step = 0; step < count; step++) {
            if (eof() || !isHex(peek())) {
                throw error("invalid hex escape");
            }
            value = (value << 4) + Character.digit(consume(), 16);
        }
        return (char) value;
    }

    private boolean matchWord(String word) {
        if (!text.startsWith(word, index)) {
            return false;
        }
        int end = index + word.length();
        if (end < length) {
            char next = text.charAt(end);
            if (Character.isLetterOrDigit(next) || next == '_') {
                return false;
            }
        }
        index = end;
        column += word.length();
        return true;
    }

    private void expect(char expected) {
        skipSpaces();
        if (eof() || peek() != expected) {
            throw error("expected '" + expected + "'");
        }
        consume();
    }

    private char peek() {
        return text.charAt(index);
    }

    private char consume() {
        char current = text.charAt(index);
        index++;
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }

    private boolean eof() {
        return index >= length;
    }

    private boolean isHex(char current) {
        return Character.digit(current, 16) >= 0;
    }

    private IllegalStateException error(String message) {
        return new IllegalStateException(message + " at " + line + ":" + column);
    }
}
