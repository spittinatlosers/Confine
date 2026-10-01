package confine.yaml;

import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Note;
import confine.node.Value;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Parser {

    private final List<Line> lines = new ArrayList<>();
    private int cursor;

    Parser(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        split(text);
    }

    Block parse() {
        Block root = new Block();
        List<String> pending = takePending(0);
        if (cursor >= lines.size()) {
            root.comments().addAll(pending);
            return root;
        }
        Line first = lines.get(cursor);
        if (first.list) {
            throw new IllegalStateException("yaml root must be a mapping");
        }
        parseMapping(root, first.indent, pending);
        if (cursor < lines.size()) {
            throw new IllegalStateException("unexpected yaml at line " + lines.get(cursor).number);
        }
        return root;
    }

    private void parseMapping(Block block, int indent, List<String> leading) {
        if (!leading.isEmpty()) {
            block.comments().addAll(leading);
        }
        while (cursor < lines.size()) {
            int mark = cursor;
            List<String> pending = takePending(indent);
            if (cursor >= lines.size()) {
                addNotes(block, pending);
                return;
            }
            Line line = lines.get(cursor);
            if (line.indent < indent || line.list) {
                cursor = mark;
                return;
            }
            if (line.indent > indent) {
                throw new IllegalStateException("unexpected yaml indent at line " + line.number);
            }
            cursor++;
            putEntry(block, line.text, line.comment, line.indent, pending);
        }
    }

    private void putEntry(
            Block block,
            String text,
            String inline,
            int indent,
            List<String> pending) {
        int colon = findColon(text);
        if (colon < 0) {
            throw new IllegalStateException("expected yaml key");
        }
        String key = unquoteKey(text.substring(0, colon).trim());
        if (key.isEmpty()) {
            throw new IllegalStateException("empty yaml key");
        }
        String rest = text.substring(colon + 1).trim();
        Node value = parseValue(rest, inline, indent, pending);
        block.put(key, value);
    }

    private Node parseValue(String rest, String inline, int indent, List<String> pending) {
        if (!rest.isEmpty()) {
            if (startsSpecial(rest)) {
                throw new IllegalStateException("unsupported yaml value");
            }
            if (rest.charAt(0) == '|' || rest.charAt(0) == '>') {
                return blockScalar(rest, indent, inline, pending);
            }
            if (rest.charAt(0) == '[' || rest.charAt(0) == '{') {
                return flow(readFlow(rest), inline, pending);
            }
            Value scalar = new Value(scalar(rest));
            scalar.setInlineComment(inline);
            scalar.comments().addAll(pending);
            return scalar;
        }
        int mark = cursor;
        List<String> inner = takePending(indent + 1);
        if (cursor >= lines.size() || lines.get(cursor).indent <= indent) {
            cursor = mark;
            Value empty = new Value(null);
            empty.setInlineComment(inline);
            empty.comments().addAll(pending);
            return empty;
        }
        Line next = lines.get(cursor);
        if (next.list) {
            int listIndent = next.indent;
            cursor = mark;
            Items items = parseSequence(listIndent);
            items.setInlineComment(inline);
            items.comments().addAll(pending);
            return items;
        }
        Block child = new Block();
        child.setInlineComment(inline);
        child.comments().addAll(pending);
        parseMapping(child, next.indent, inner);
        return child;
    }

    private Items parseSequence(int indent) {
        Items items = new Items();
        while (cursor < lines.size()) {
            int mark = cursor;
            List<String> pending = takePending(indent);
            if (cursor >= lines.size()) {
                cursor = mark;
                return items;
            }
            Line line = lines.get(cursor);
            if (line.indent != indent || !line.list) {
                cursor = mark;
                return items;
            }
            cursor++;
            items.add(parseItem(line, pending));
        }
        return items;
    }

    private Node parseItem(Line line, List<String> pending) {
        String after = afterDash(line.text);
        if (after.isEmpty()) {
            int mark = cursor;
            List<String> inner = takePending(line.indent + 1);
            if (cursor >= lines.size() || lines.get(cursor).indent <= line.indent) {
                cursor = mark;
                Value empty = new Value(null);
                empty.comments().addAll(pending);
                empty.setInlineComment(line.comment);
                return empty;
            }
            Line next = lines.get(cursor);
            if (next.list) {
                Items nested = parseSequence(next.indent);
                nested.comments().addAll(pending);
                return nested;
            }
            Block child = new Block();
            child.comments().addAll(pending);
            child.setInlineComment(line.comment);
            parseMapping(child, next.indent, inner);
            return child;
        }
        if (startsSpecial(after)) {
            throw new IllegalStateException("unsupported yaml value");
        }
        if (after.charAt(0) == '|' || after.charAt(0) == '>') {
            return blockScalar(after, line.indent, line.comment, pending);
        }
        if (after.charAt(0) == '[' || after.charAt(0) == '{') {
            return flow(readFlow(after), line.comment, pending);
        }
        if (findColon(after) >= 0) {
            Block child = new Block();
            child.comments().addAll(pending);
            putEntry(child, after, line.comment, columnAfterDash(line), Collections.emptyList());
            parseMapping(child, columnAfterDash(line), Collections.emptyList());
            return child;
        }
        Value scalar = new Value(scalar(after));
        scalar.comments().addAll(pending);
        scalar.setInlineComment(line.comment);
        return scalar;
    }

    private Node blockScalar(
            String indicator,
            int parentIndent,
            String inline,
            List<String> pending) {
        boolean folded = indicator.charAt(0) == '>';
        char chomp = ' ';
        if (indicator.length() > 1) {
            char mark = indicator.charAt(1);
            if (mark == '+' || mark == '-') {
                chomp = mark;
            }
            String tail = indicator.substring(chomp == ' ' ? 1 : 2).trim();
            if (!tail.isEmpty()) {
                throw new IllegalStateException("unsupported yaml block scalar");
            }
        }
        List<String> body = new ArrayList<>();
        Integer contentIndent = null;
        while (cursor < lines.size()) {
            Line line = lines.get(cursor);
            if (line.blank) {
                int look = cursor + 1;
                while (look < lines.size() && lines.get(look).blank) {
                    look++;
                }
                if (look >= lines.size() || lines.get(look).indent <= parentIndent) {
                    break;
                }
                body.add("");
                cursor++;
                continue;
            }
            if (line.indent <= parentIndent) {
                break;
            }
            if (contentIndent == null || line.indent < contentIndent) {
                contentIndent = line.indent;
            }
            body.add(line.raw);
            cursor++;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < body.size(); index++) {
            if (index > 0) {
                builder.append(folded ? fold(body, index) : '\n');
            }
            String row = body.get(index);
            if (row.isEmpty()) {
                continue;
            }
            int cut = Math.min(contentIndent == null ? 0 : contentIndent, row.length());
            builder.append(row.substring(cut));
        }
        if (chomp == '+') {
            builder.append('\n');
        }
        Value value = new Value(builder.toString());
        value.setInlineComment(inline);
        value.comments().addAll(pending);
        return value;
    }

    private char fold(List<String> body, int index) {
        if (body.get(index).isEmpty() || body.get(index - 1).isEmpty()) {
            return '\n';
        }
        return ' ';
    }

    private String readFlow(String first) {
        StringBuilder builder = new StringBuilder(first);
        while (depth(builder) > 0) {
            if (cursor >= lines.size()) {
                throw new IllegalStateException("unterminated yaml flow");
            }
            Line line = lines.get(cursor);
            cursor++;
            builder.append('\n').append(line.raw.trim());
        }
        return builder.toString();
    }

    private Node flow(String text, String inline, List<String> pending) {
        Flow reader = new Flow(text);
        Node node = reader.parse();
        node.setInlineComment(inline);
        node.comments().addAll(pending);
        reader.finish();
        return node;
    }

    private List<String> takePending(int indent) {
        List<String> pending = new ArrayList<>();
        while (cursor < lines.size()) {
            Line line = lines.get(cursor);
            if (line.blank) {
                int look = cursor + 1;
                while (look < lines.size() && lines.get(look).blank) {
                    look++;
                }
                if (look < lines.size() && lines.get(look).indent < indent) {
                    return pending;
                }
                cursor++;
                continue;
            }
            if (line.commentOnly) {
                if (line.indent < indent) {
                    return pending;
                }
                pending.add(line.comment);
                cursor++;
                continue;
            }
            break;
        }
        return pending;
    }

    private void addNotes(Block block, List<String> pending) {
        for (String comment : pending) {
            block.add(new Note(comment));
        }
    }

    private void split(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        int number = 1;
        int start = 0;
        while (start <= normalized.length()) {
            int end = normalized.indexOf('\n', start);
            if (end < 0) {
                end = normalized.length();
            }
            accept(normalized.substring(start, end), number);
            if (end >= normalized.length()) {
                break;
            }
            start = end + 1;
            number++;
        }
    }

    private void accept(String raw, int number) {
        int indent = 0;
        while (indent < raw.length()) {
            char current = raw.charAt(indent);
            if (current == '\t') {
                throw new IllegalStateException("yaml tabs are not allowed at line " + number);
            }
            if (current != ' ') {
                break;
            }
            indent++;
        }
        String content = raw.substring(indent);
        String trimmed = content.trim();
        if (trimmed.equals("---") || trimmed.equals("...")) {
            return;
        }
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '%') {
            throw new IllegalStateException("yaml directive is not supported");
        }
        int hash = hash(content);
        String text = hash < 0 ? content.trim() : content.substring(0, hash).trim();
        String comment = hash < 0 ? null : content.substring(hash + 1).trim();
        boolean blank = text.isEmpty() && comment == null;
        boolean commentOnly = text.isEmpty() && comment != null;
        boolean list = text.equals("-") || text.startsWith("- ");
        lines.add(new Line(number, indent, raw, text, comment, blank, commentOnly, list));
    }

    private int hash(String content) {
        boolean single = false;
        boolean quote = false;
        boolean escape = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (escape) {
                escape = false;
                continue;
            }
            if (current == '\\' && quote) {
                escape = true;
                continue;
            }
            if (current == '\'' && !quote) {
                single = !single;
                continue;
            }
            if (current == '"' && !single) {
                quote = !quote;
                continue;
            }
            if (current == '#' && !single && !quote) {
                return index;
            }
        }
        return -1;
    }

    private int findColon(String text) {
        boolean single = false;
        boolean quote = false;
        boolean escape = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (escape) {
                escape = false;
                continue;
            }
            if (current == '\\' && quote) {
                escape = true;
                continue;
            }
            if (current == '\'' && !quote) {
                single = !single;
                continue;
            }
            if (current == '"' && !single) {
                quote = !quote;
                continue;
            }
            if (single || quote || current != ':') {
                continue;
            }
            if (index == text.length() - 1 || text.charAt(index + 1) == ' ') {
                return index;
            }
        }
        return -1;
    }

    private String unquoteKey(String key) {
        if (key.length() >= 2 && key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
            return unescape(key.substring(1, key.length() - 1));
        }
        if (key.length() >= 2 && key.charAt(0) == '\'' && key.charAt(key.length() - 1) == '\'') {
            return key.substring(1, key.length() - 1).replace("''", "'");
        }
        return key;
    }

    private String afterDash(String text) {
        if (text.equals("-")) {
            return "";
        }
        return text.substring(2).trim();
    }

    private int columnAfterDash(Line line) {
        int index = line.indent;
        if (index < line.raw.length() && line.raw.charAt(index) == '-') {
            index++;
        }
        if (index < line.raw.length() && line.raw.charAt(index) == ' ') {
            index++;
        }
        return index;
    }

    private boolean startsSpecial(String text) {
        char current = text.charAt(0);
        return current == '&' || current == '*' || current == '!' || current == '?';
    }

    private int depth(CharSequence text) {
        int level = 0;
        boolean single = false;
        boolean quote = false;
        boolean escape = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '\n') {
                continue;
            }
            if (escape) {
                escape = false;
                continue;
            }
            if (current == '\\' && quote) {
                escape = true;
                continue;
            }
            if (current == '\'' && !quote) {
                single = !single;
                continue;
            }
            if (current == '"' && !single) {
                quote = !quote;
                continue;
            }
            if (single || quote) {
                continue;
            }
            if (current == '#' ) {
                while (index + 1 < text.length() && text.charAt(index + 1) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == '{' || current == '[') {
                level++;
            } else if (current == '}' || current == ']') {
                level--;
            }
        }
        return level;
    }

    private Object scalar(String text) {
        if (text.equals("~") || text.equals("null") || text.equals("Null") || text.equals("NULL")) {
            return null;
        }
        if (text.equals("true") || text.equals("True") || text.equals("TRUE")) {
            return true;
        }
        if (text.equals("false") || text.equals("False") || text.equals("FALSE")) {
            return false;
        }
        if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            return unescape(text.substring(1, text.length() - 1));
        }
        if (text.length() >= 2
                && text.charAt(0) == '\''
                && text.charAt(text.length() - 1) == '\'') {
            return text.substring(1, text.length() - 1).replace("''", "'");
        }
        Object number = number(text);
        if (number != null) {
            return number;
        }
        return text;
    }

    private String unescape(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current != '\\' || index + 1 >= text.length()) {
                builder.append(current);
                continue;
            }
            char next = text.charAt(++index);
            switch (next) {
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case '"':
                    builder.append('"');
                    break;
                case '\\':
                    builder.append('\\');
                    break;
                case '/':
                    builder.append('/');
                    break;
                case 'u':
                    if (index + 4 >= text.length()) {
                        throw new IllegalStateException("invalid yaml escape");
                    }
                    builder.append((char) Integer.parseInt(text.substring(index + 1, index + 5), 16));
                    index += 4;
                    break;
                default:
                    builder.append(next);
                    break;
            }
        }
        return builder.toString();
    }

    private Object number(String text) {
        String raw = text.replace("_", "");
        if (raw.isEmpty()) {
            return null;
        }
        int start = 0;
        if (raw.charAt(0) == '+' || raw.charAt(0) == '-') {
            start = 1;
        }
        if (start >= raw.length()) {
            return null;
        }
        if (raw.startsWith("0x", start) || raw.startsWith("0X", start)) {
            try {
                return fit(new BigInteger(raw.substring(start + 2), 16), raw.charAt(0) == '-');
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        boolean digit = false;
        boolean dot = false;
        boolean exp = false;
        for (int index = start; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current >= '0' && current <= '9') {
                digit = true;
                continue;
            }
            if (current == '.' && !dot && !exp) {
                dot = true;
                continue;
            }
            if ((current == 'e' || current == 'E') && !exp && digit) {
                exp = true;
                if (index + 1 < raw.length() && (raw.charAt(index + 1) == '+' || raw.charAt(index + 1) == '-')) {
                    index++;
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
            return fit(new BigInteger(raw), false);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object fit(BigInteger number, boolean negative) {
        BigInteger value = negative ? number.negate() : number;
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                && value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
            return value.intValue();
        }
        if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0
                && value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0) {
            return value.longValue();
        }
        return value;
    }

    private static final class Line {
        private final int number;
        private final int indent;
        private final String raw;
        private final String text;
        private final String comment;
        private final boolean blank;
        private final boolean commentOnly;
        private final boolean list;

        private Line(
                int number,
                int indent,
                String raw,
                String text,
                String comment,
                boolean blank,
                boolean commentOnly,
                boolean list) {
            this.number = number;
            this.indent = indent;
            this.raw = raw;
            this.text = text;
            this.comment = comment;
            this.blank = blank;
            this.commentOnly = commentOnly;
            this.list = list;
        }
    }

    private final class Flow {
        private final String text;
        private int index;

        private Flow(String text) {
            this.text = text;
        }

        private Node parse() {
            skip();
            if (index >= text.length()) {
                throw new IllegalStateException("empty yaml flow");
            }
            char current = text.charAt(index);
            if (current == '[') {
                return array();
            }
            if (current == '{') {
                return object();
            }
            return new Value(scalar(readScalar()));
        }

        private Items array() {
            expect('[');
            Items items = new Items();
            skip();
            if (match(']')) {
                return items;
            }
            while (index < text.length()) {
                items.add(parse());
                skip();
                if (match(']')) {
                    return items;
                }
                expect(',');
                skip();
            }
            throw new IllegalStateException("unterminated yaml flow");
        }

        private Block object() {
            expect('{');
            Block block = new Block();
            skip();
            if (match('}')) {
                return block;
            }
            while (index < text.length()) {
                skip();
                String key = readKey();
                skip();
                expect(':');
                Node value = parse();
                block.put(key, value);
                skip();
                if (match('}')) {
                    return block;
                }
                expect(',');
            }
            throw new IllegalStateException("unterminated yaml flow");
        }

        private String readKey() {
            skip();
            if (index < text.length() && (text.charAt(index) == '"' || text.charAt(index) == '\'')) {
                return String.valueOf(scalar(readScalar()));
            }
            int colon = findColon(text.substring(index));
            if (colon < 0) {
                throw new IllegalStateException("expected yaml key");
            }
            String key = text.substring(index, index + colon).trim();
            index += colon;
            return key;
        }

        private String readScalar() {
            skip();
            if (index >= text.length()) {
                throw new IllegalStateException("expected yaml value");
            }
            char current = text.charAt(index);
            if (current == '"' || current == '\'') {
                return readQuoted(current);
            }
            int start = index;
            while (index < text.length()) {
                char next = text.charAt(index);
                if (next == ','
                        || next == '}'
                        || next == ']'
                        || next == ':'
                        || next == '\n'
                        || next == '#') {
                    break;
                }
                index++;
            }
            return text.substring(start, index).trim();
        }

        private String readQuoted(char quote) {
            int start = index;
            index++;
            boolean escape = false;
            while (index < text.length()) {
                char current = text.charAt(index);
                if (escape) {
                    escape = false;
                    index++;
                    continue;
                }
                if (current == '\\' && quote == '"') {
                    escape = true;
                    index++;
                    continue;
                }
                if (current == quote) {
                    if (quote == '\''
                            && index + 1 < text.length()
                            && text.charAt(index + 1) == '\'') {
                        index += 2;
                        continue;
                    }
                    index++;
                    return text.substring(start, index);
                }
                index++;
            }
            throw new IllegalStateException("unterminated yaml string");
        }

        private void skip() {
            while (index < text.length()) {
                char current = text.charAt(index);
                if (current == ' ' || current == '\n' || current == '\t' || current == '\r') {
                    index++;
                    continue;
                }
                if (current == '#') {
                    while (index < text.length() && text.charAt(index) != '\n') {
                        index++;
                    }
                    continue;
                }
                return;
            }
        }

        private void expect(char current) {
            skip();
            if (index >= text.length() || text.charAt(index) != current) {
                throw new IllegalStateException("expected '" + current + "' in yaml flow");
            }
            index++;
        }

        private boolean match(char current) {
            if (index < text.length() && text.charAt(index) == current) {
                index++;
                return true;
            }
            return false;
        }

        private void finish() {
            skip();
            if (index < text.length()) {
                throw new IllegalStateException("trailing yaml flow");
            }
        }
    }
}
