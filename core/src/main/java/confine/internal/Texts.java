package confine.internal;

import java.io.IOException;
import java.io.Reader;

public final class Texts {

    private Texts() {
    }

    public static String read(Reader reader) throws IOException {
        if (reader == null) {
            throw new NullPointerException("reader");
        }
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) >= 0) {
            builder.append(buffer, 0, count);
        }
        if (builder.length() != 0 && builder.charAt(0) == '\uFEFF') {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
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
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }

                    break;
            }
        }
        return builder.toString();
    }

    public static boolean blank(String value) {
        if (value == null) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public static String spaces(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(' ');
        }
        return builder.toString();
    }
}
