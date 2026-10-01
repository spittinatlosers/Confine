package confine;

import confine.check.ConfigPredicate;
import confine.check.MaxValidator;
import confine.check.MinValidator;
import confine.check.NotBlankValidator;
import confine.check.NotNullValidator;
import confine.check.OneOfValidator;
import confine.check.PredicateValidator;
import confine.check.RangeValidator;
import confine.check.RegexValidator;
import confine.internal.Texts;
import confine.node.Block;
import confine.node.Items;
import confine.node.Node;
import confine.node.Nodes;
import confine.node.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class KeyBuilder {

    private final ScopeBuilder owner;
    private final Block section;
    private final String name;
    private final KeyDefinition definition;
    private final List<String> comments = new ArrayList<>();
    private String inline;
    private Node node;
    private Double minimum;
    private Double maximum;

    KeyBuilder(
            ScopeBuilder owner,
            Block section,
            List<String> prefix,
            String name,
            ProgrammaticSchema schema) {
        if (owner == null) {
            throw new NullPointerException("owner");
        }
        if (section == null) {
            throw new NullPointerException("section");
        }
        if (name == null || Texts.blank(name)) {
            throw new IllegalArgumentException("name");
        }
        this.owner = owner;
        this.section = section;
        this.name = name;
        List<String> parts = new ArrayList<>();
        if (prefix != null) {
            parts.addAll(prefix);
        }
        parts.add(name);
        this.definition = new KeyDefinition(Nodes.join(parts));
        schema.add(definition);
    }

    public KeyBuilder string(String value) {
        store(new Value(value), String.class);
        return this;
    }

    public KeyBuilder bool(boolean value) {
        store(new Value(value), Boolean.class);
        return this;
    }

    public KeyBuilder integer(int value) {
        store(new Value(value), Integer.class);
        return this;
    }

    public KeyBuilder longValue(long value) {
        store(new Value(value), Long.class);
        return this;
    }

    public KeyBuilder decimal(double value) {
        store(new Value(value), Double.class);
        return this;
    }

    public KeyBuilder number(BigDecimal value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        store(new Value(value), BigDecimal.class);
        return this;
    }

    public KeyBuilder nil() {
        store(new Value(null), Object.class);
        return this;
    }

    public KeyBuilder list(Collection<?> values) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        Node created = Nodes.fromPlain(values);
        if (!(created instanceof Items)) {
            created = new Items();
        }
        store(created, List.class);
        return this;
    }

    public KeyBuilder array(Object... values) {
        if (values == null) {
            throw new NullPointerException("values");
        }
        List<Object> copy = new ArrayList<>();
        for (Object value : values) {
            copy.add(value);
        }
        return list(copy);
    }

    public KeyBuilder node(Node value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        store(value.copy(), value.getClass());
        return this;
    }

    public KeyBuilder comment(String line) {
        if (line == null) {
            throw new NullPointerException("line");
        }
        comments.add(line);
        refresh();
        return this;
    }

    public KeyBuilder inline(String comment) {
        this.inline = comment;
        refresh();
        return this;
    }

    public KeyBuilder min(double min) {
        this.minimum = min;
        refreshRange();
        return this;
    }

    public KeyBuilder max(double max) {
        this.maximum = max;
        refreshRange();
        return this;
    }

    public KeyBuilder regex(String pattern) {
        definition.add(new RegexValidator(pattern, ""));
        return this;
    }

    public KeyBuilder notNull() {
        definition.setRequired(true);
        definition.add(new NotNullValidator(""));
        return this;
    }

    public KeyBuilder notBlank() {
        definition.setRequired(true);
        definition.add(new NotBlankValidator(""));
        return this;
    }

    public KeyBuilder oneOf(String... allowed) {
        definition.add(new OneOfValidator(allowed, ""));
        return this;
    }

    public KeyBuilder predicate(ConfigPredicate<?> predicate) {
        definition.add(new PredicateValidator(predicate, ""));
        return this;
    }

    public ScopeBuilder end() {
        if (node == null) {
            throw new IllegalStateException("key " + name + " has no value");
        }
        return owner;
    }

    private void store(Node created, Class<?> type) {
        this.node = created;
        definition.setType(type);
        refresh();
        section.put(name, created);
    }

    private void refresh() {
        if (node == null) {
            return;
        }
        node.comments().clear();
        node.comments().addAll(comments);
        node.setInlineComment(inline);
    }

    private void refreshRange() {
        if (minimum != null && maximum != null) {
            definition.add(new RangeValidator(minimum, maximum, ""));
            return;
        }
        if (minimum != null) {
            definition.add(new MinValidator(minimum, ""));
        }
        if (maximum != null) {
            definition.add(new MaxValidator(maximum, ""));
        }
    }
}
