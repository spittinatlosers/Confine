package confine;

import confine.internal.Texts;
import confine.node.Block;
import confine.node.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SchemaBuilder extends ScopeBuilder {

    private final Block root = new Block();
    private final ProgrammaticSchema schema = new ProgrammaticSchema();

    public ProgrammaticSchema schema() {
        return schema;
    }

    @Override
    public KeyBuilder key(String name) {
        return newKey(root, Collections.emptyList(), name, schema);
    }

    @Override
    public SectionBuilder section(String name) {
        if (name == null || Texts.blank(name)) {
            throw new IllegalArgumentException("name");
        }
        Block child = new Block();
        root.put(name, child);
        List<String> path = new ArrayList<>();
        path.add(name);
        return new SectionBuilder(this, null, child, path);
    }

    @Override
    public SchemaBuilder comment(String line) {
        if (line == null) {
            throw new NullPointerException("line");
        }
        root.addComment(line);
        return this;
    }

    @Override
    public SchemaBuilder inline(String comment) {
        root.setInlineComment(comment);
        return this;
    }

    @Override
    public SchemaBuilder header(String line) {
        return comment(line);
    }

    public SchemaBuilder version(int version) {
        return version("config-version", version);
    }

    public SchemaBuilder version(String key, int version) {
        if (key == null || Texts.blank(key)) {
            throw new IllegalArgumentException("key");
        }
        root.put(key, new Value(version));
        return this;
    }

    @Override
    public FluentDocument build() {
        return new FluentDocument(root, schema);
    }
}
