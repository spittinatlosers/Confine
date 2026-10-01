package confine.node;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Nodes {

    private Nodes() {
    }

    public static List<String> split(String path) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escape = false;
        for (int index = 0; index < path.length(); index++) {
            char currentChar = path.charAt(index);
            if (escape) {
                current.append(currentChar);
                escape = false;
                continue;
            }
            if (currentChar == '\\') {
                escape = true;
                continue;
            }
            if (currentChar == '.') {
                addPart(parts, current);
                continue;
            }
            current.append(currentChar);
        }
        if (escape) {
            current.append('\\');
        }
        addPart(parts, current);
        return Collections.unmodifiableList(new ArrayList<>(parts));
    }

    public static String join(List<String> parts) {
        if (parts == null) {
            throw new NullPointerException("parts");
        }
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                throw new IllegalStateException("empty path segment");
            }
            if (builder.length() != 0) {
                builder.append('.');
            }
            builder.append(escape(part));
        }
        return builder.toString();
    }

    public static String escape(String part) {
        if (part == null) {
            throw new NullPointerException("part");
        }
        return part.replace("\\", "\\\\").replace(".", "\\.");
    }

    public static Node find(Block root, String path) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        List<String> parts = split(path);
        if (parts.isEmpty()) {
            return root;
        }
        Node current = root;
        for (String part : parts) {
            if (!(current instanceof Block)) {
                return null;
            }
            Block sectionNode = (Block) current;
            current = sectionNode.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public static void set(Block root, String path, Node value) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        List<String> parts = split(path);
        if (parts.isEmpty()) {
            throw new IllegalStateException("empty path");
        }
        Block current = root;
        for (int index = 0; index < parts.size() - 1; index++) {
            String part = parts.get(index);
            Node child = current.get(part);
            if (child == null) {
                Block created = new Block();
                current.put(part, created);
                current = created;
                continue;
            }
            if (!(child instanceof Block)) {
                throw new IllegalStateException("path conflicts at " + part);
            }
            Block sectionNode = (Block) child;
            current = sectionNode;
        }
        current.put(parts.get(parts.size() - 1), value);
    }

    public static Node remove(Block root, String path) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        List<String> parts = split(path);
        if (parts.isEmpty()) {
            throw new IllegalStateException("empty path");
        }
        Block current = root;
        for (int index = 0; index < parts.size() - 1; index++) {
            Node child = current.get(parts.get(index));
            if (!(child instanceof Block)) {
                return null;
            }
            Block sectionNode = (Block) child;
            current = sectionNode;
        }
        return current.remove(parts.get(parts.size() - 1));
    }

    public static Object plain(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Value) {
            Value valueNode = (Value) node;
            return valueNode.value();
        }
        if (node instanceof Items) {
            Items listNode = (Items) node;
            List<Object> values = new ArrayList<>();
            for (Node element : listNode.elements()) {
                if (element instanceof Note) {
                    continue;
                }
                values.add(plain(element));
            }
            return values;
        }
        if (node instanceof Block) {
            Block sectionNode = (Block) node;
            Map<String, Object> values = new LinkedHashMap<>();
            for (Node child : sectionNode.ordered()) {
                if (child instanceof Note || child.name() == null) {
                    continue;
                }
                values.put(child.name(), plain(child));
            }
            return values;
        }
        if (node instanceof Note) {
            Note commentNode = (Note) node;
            return commentNode.text();
        }
        return null;
    }

    public static Node fromPlain(Object value) {
        if (value instanceof Node) {
            Node configNode = (Node) value;
            return configNode.copy();
        }
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            Block section = new Block();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    throw new IllegalStateException("null map key");
                }
                section.put(String.valueOf(entry.getKey()), fromPlain(entry.getValue()));
            }
            return section;
        }
        if (value instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) value;
            Items list = new Items();
            for (Object element : collection) {
                list.add(fromPlain(element));
            }
            return list;
        }
        if (value != null && value.getClass().isArray()) {
            Items list = new Items();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                list.add(fromPlain(Array.get(value, index)));
            }
            return list;
        }
        return new Value(value);
    }

    private static void addPart(List<String> parts, StringBuilder current) {
        if (current.length() == 0) {
            throw new IllegalStateException("empty path segment");
        }
        parts.add(current.toString());
        current.setLength(0);
    }
}
