package confine.change;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Node;
import confine.node.Value;

public final class VersionKeys {

    public static final String DEFAULT = "config-version";

    private VersionKeys() {
    }

    public static int read(Block root, String key, int fallback) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        String selected = key == null || Texts.blank(key) ? DEFAULT : key;
        Node node = root.get(selected);
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            return fallback;
        }
        Value valueNode = (Value) node;
        Object raw = valueNode.value();
        if (raw instanceof Number) {
            Number number = (Number) raw;
            return number.intValue();
        }
        if (raw instanceof String) {
            String text = (String) raw;
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }
        return fallback;
    }

    public static void write(Block root, String key, int version) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        String selected = key == null || Texts.blank(key) ? DEFAULT : key;
        Value value = new Value(version);
        Node existing = root.get(selected);
        if (existing != null) {
            value.comments().addAll(existing.comments());
            value.setInlineComment(existing.inlineComment());
        }
        root.put(selected, value);
    }
}
