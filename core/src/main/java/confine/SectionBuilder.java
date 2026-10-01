package confine;

import confine.internal.Texts;
import confine.node.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SectionBuilder extends ScopeBuilder {

    private final SchemaBuilder schemaBuilder;
    private final SectionBuilder parent;
    private final Block node;
    private final List<String> path;

    SectionBuilder(
            SchemaBuilder schemaBuilder,
            SectionBuilder parent,
            Block node,
            List<String> path) {
        if (schemaBuilder == null) {
            throw new NullPointerException("schemaBuilder");
        }
        if (node == null) {
            throw new NullPointerException("node");
        }
        this.schemaBuilder = schemaBuilder;
        this.parent = parent;
        this.node = node;
        this.path = path == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(path));
    }

    @Override
    public KeyBuilder key(String name) {
        return newKey(node, path, name, schemaBuilder.schema());
    }

    @Override
    public SectionBuilder section(String name) {
        if (name == null || Texts.blank(name)) {
            throw new IllegalArgumentException("name");
        }
        Block child = new Block();
        node.put(name, child);
        List<String> childPath = new ArrayList<>(path);
        childPath.add(name);
        return new SectionBuilder(schemaBuilder, this, child, childPath);
    }

    @Override
    public SectionBuilder comment(String line) {
        if (line == null) {
            throw new NullPointerException("line");
        }
        node.addComment(line);
        return this;
    }

    @Override
    public SectionBuilder inline(String comment) {
        node.setInlineComment(comment);
        return this;
    }

    @Override
    public ScopeBuilder header(String line) {
        return schemaBuilder.header(line);
    }

    public ScopeBuilder end() {
        if (parent != null) {
            return parent;
        }
        return schemaBuilder;
    }

    @Override
    public FluentDocument build() {
        return schemaBuilder.build();
    }
}
