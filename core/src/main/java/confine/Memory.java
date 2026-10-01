package confine;

import confine.internal.Scalars;
import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Nodes;
import confine.node.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Memory implements Section {

    private final Memory parent;
    private final String name;
    private final Block node;

    private Memory(Memory parent, String name, Block node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        this.parent = parent;
        this.name = name == null ? "" : name;
        this.node = node;
    }

    public static Memory create() {
        return new Memory(null, "", new Block());
    }

    public static Memory wrap(Block node) {
        return new Memory(null, "", node);
    }

    @Override
    public Map<String, Object> toMap() {
        Object plain = Nodes.plain(node);
        if (plain instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) plain;
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return copy;
        }
        return new LinkedHashMap<>();
    }

    @Override
    public boolean contains(String path) {
        return contains(path, false);
    }

    @Override
    public boolean contains(String path, boolean ignoreNull) {
        Node found = locate(path);
        if (found == null) {
            return false;
        }
        if (ignoreNull && found instanceof Value && ((Value) found).isNull()) {
            return false;
        }
        return true;
    }

    @Override
    public String currentPath() {
        if (parent == null || name.isEmpty()) {
            return "";
        }
        String parentPath = parent.currentPath();
        if (parentPath.isEmpty()) {
            return Nodes.escape(name);
        }
        return parentPath + "." + Nodes.escape(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Section root() {
        Memory current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    @Override
    public Section parent() {
        return parent;
    }

    @Override
    public Object get(String path) {
        return get(path, (Object) null);
    }

    @Override
    public Object get(String path, Object defaultValue) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (path.isEmpty()) {
            return Nodes.plain(node);
        }
        Node found = Nodes.find(node, path);
        if (found == null) {
            return defaultValue;
        }
        return Nodes.plain(found);
    }

    @Override
    public void set(String path, Object value) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (value instanceof Section) {
            Section section = (Section) value;
            Nodes.set(node, path, section.node().copy());
            return;
        }
        Nodes.set(node, path, Nodes.fromPlain(value));
    }

    @Override
    public void remove(String path) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        Nodes.remove(node, path);
    }

    @Override
    public Section createSection(String path) {
        List<String> parts = Nodes.split(path);
        if (parts.isEmpty()) {
            return this;
        }
        Memory current = this;
        for (String part : parts) {
            Node child = current.node.get(part);
            if (child instanceof Block) {
                Block sectionNode = (Block) child;
                current = new Memory(current, part, sectionNode);
                continue;
            }
            if (child != null) {
                throw new IllegalStateException("path conflicts at " + part);
            }
            Block created = new Block();
            current.node.put(part, created);
            current = new Memory(current, part, created);
        }
        return current;
    }

    @Override
    public Section section(String path) {
        List<String> parts = Nodes.split(path);
        if (parts.isEmpty()) {
            return this;
        }
        Memory current = this;
        for (String part : parts) {
            Node child = current.node.get(part);
            if (!(child instanceof Block)) {
                return null;
            }
            Block sectionNode = (Block) child;
            current = new Memory(current, part, sectionNode);
        }
        return current;
    }

    @Override
    public boolean isSection(String path) {
        return locate(path) instanceof Block;
    }

    @Override
    public String getString(String path) {
        return getString(path, null);
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String) {
            String text = (String) value;
            return text;
        }
        return Scalars.asText(value, defaultValue);
    }

    @Override
    public boolean isString(String path) {
        return get(path) instanceof String;
    }

    @Override
    public int getInt(String path) {
        return getInt(path, 0);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        return Scalars.toInt(get(path), defaultValue);
    }

    @Override
    public boolean isInt(String path) {
        Object value = get(path);
        return value instanceof Integer || value instanceof Short || value instanceof Byte;
    }

    @Override
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return Scalars.toBoolean(get(path), defaultValue);
    }

    @Override
    public boolean isBoolean(String path) {
        return get(path) instanceof Boolean;
    }

    @Override
    public double getDouble(String path) {
        return getDouble(path, 0D);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        return Scalars.toDouble(get(path), defaultValue);
    }

    @Override
    public boolean isDouble(String path) {
        Object value = get(path);
        return value instanceof Double || value instanceof Float;
    }

    @Override
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    @Override
    public long getLong(String path, long defaultValue) {
        return Scalars.toLong(get(path), defaultValue);
    }

    @Override
    public boolean isLong(String path) {
        return get(path) instanceof Long;
    }

    @Override
    public float getFloat(String path) {
        return getFloat(path, 0F);
    }

    @Override
    public float getFloat(String path, float defaultValue) {
        return Scalars.toFloat(get(path), defaultValue);
    }

    @Override
    public boolean isFloat(String path) {
        return get(path) instanceof Float;
    }

    @Override
    public byte getByte(String path) {
        return getByte(path, (byte) 0);
    }

    @Override
    public byte getByte(String path, byte defaultValue) {
        return Scalars.toByte(get(path), defaultValue);
    }

    @Override
    public boolean isByte(String path) {
        return get(path) instanceof Byte;
    }

    @Override
    public short getShort(String path) {
        return getShort(path, (short) 0);
    }

    @Override
    public short getShort(String path, short defaultValue) {
        return Scalars.toShort(get(path), defaultValue);
    }

    @Override
    public boolean isShort(String path) {
        return get(path) instanceof Short;
    }

    @Override
    public char getChar(String path) {
        return getChar(path, '\0');
    }

    @Override
    public char getChar(String path, char defaultValue) {
        return Scalars.toChar(get(path), defaultValue);
    }

    @Override
    public boolean isChar(String path) {
        Object value = get(path);
        if (value instanceof Character) {
            return true;
        }
        return value instanceof String && ((String) value).length() == 1;
    }

    @Override
    public <T> List<T> getList(String path, Class<T> type) {
        return getList(path, type, null);
    }

    @Override
    public <T> List<T> getList(String path, Class<T> type, List<T> defaultValue) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        Object value = get(path);
        if (!(value instanceof List<?>)) {
            return defaultValue;
        }
        List<?> list = (List<?>) value;
        List<T> result = new ArrayList<>();
        Class<?> wrapped = Scalars.wrap(type);
        for (Object element : list) {
            Scalars.Coercion coercion = Scalars.tryCoerce(element, type);
            if (!coercion.success()) {
                continue;
            }
            Object coerced = coercion.value();
            if (coerced == null) {
                result.add(null);
                continue;
            }
            result.add(cast(wrapped, coerced));
        }
        return result;
    }

    @Override
    public boolean isList(String path) {
        return get(path) instanceof List<?>;
    }

    @Override
    public List<String> getStringList(String path) {
        List<String> values = getList(path, String.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        List<Integer> values = getList(path, Integer.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Boolean> getBooleanList(String path) {
        List<Boolean> values = getList(path, Boolean.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Double> getDoubleList(String path) {
        List<Double> values = getList(path, Double.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Float> getFloatList(String path) {
        List<Float> values = getList(path, Float.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Long> getLongList(String path) {
        List<Long> values = getList(path, Long.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Byte> getByteList(String path) {
        List<Byte> values = getList(path, Byte.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Character> getCharacterList(String path) {
        List<Character> values = getList(path, Character.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public List<Short> getShortList(String path) {
        List<Short> values = getList(path, Short.class, new ArrayList<>());
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        return get(path, type, null);
    }

    @Override
    public <T> T get(String path, Class<T> type, T defaultValue) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (Section.class.isAssignableFrom(type)) {
            Section section = section(path);
            if (section == null) {
                return defaultValue;
            }
            return type.cast(section);
        }
        Object value = get(path);
        if (value == null) {
            return defaultValue;
        }
        Scalars.Coercion coercion = Scalars.tryCoerce(value, type);
        if (!coercion.success() || coercion.value() == null) {
            if (type.isInstance(value)) {
                return type.cast(value);
            }
            return defaultValue;
        }
        return cast(Scalars.wrap(type), coercion.value());
    }

    @Override
    public List<Section> getSectionList(String path) {
        Node found = locate(path);
        if (!(found instanceof Items)) {
            return new ArrayList<>();
        }
        Items listNode = (Items) found;
        List<Section> sections = new ArrayList<>();
        int index = 0;
        for (Node element : listNode.elements()) {
            if (element instanceof Block) {
                Block sectionNode = (Block) element;
                sections.add(new Memory(this, name + "[" + index + "]", sectionNode));
            }
            index++;
        }
        return sections;
    }

    @Override
    public List<String> comments(String path) {
        Node found = locate(path);
        if (found == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(found.comments()));
    }

    @Override
    public void comments(String path, List<String> comments) {
        if (comments == null) {
            throw new NullPointerException("comments");
        }
        Node found = locate(path);
        if (found == null) {
            throw new IllegalStateException("missing path " + path);
        }
        found.comments().clear();
        found.comments().addAll(comments);
    }

    @Override
    public String inlineComment(String path) {
        Node found = locate(path);
        if (found == null) {
            return null;
        }
        return found.inlineComment();
    }

    @Override
    public void inlineComment(String path, String comment) {
        Node found = locate(path);
        if (found == null) {
            throw new IllegalStateException("missing path " + path);
        }
        found.setInlineComment(comment);
    }

    @Override
    public Set<String> keys() {
        return keys(false);
    }

    @Override
    public Set<String> keys(boolean deep) {
        if (!deep) {
            return node.keys();
        }
        Set<String> result = new LinkedHashSet<>();
        collect(node, "", result);
        return result;
    }

    @Override
    public Block node() {
        return node;
    }

    private Node locate(String path) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (path.isEmpty()) {
            return node;
        }
        return Nodes.find(node, path);
    }

    private void collect(Block section, String prefix, Set<String> result) {
        for (Node child : section.ordered()) {
            if (child.name() == null) {
                continue;
            }
            String path = prefix.isEmpty() ? Nodes.escape(child.name()) : prefix
                    + "."
                    + Nodes.escape(child.name());
            result.add(path);
            if (child instanceof Block) {
                Block nested = (Block) child;
                collect(nested, path, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Class<?> type, Object value) {
        return (T) type.cast(value);
    }
}
