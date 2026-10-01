package confine;

import confine.node.Block;

import java.util.List;

public abstract class ScopeBuilder {

    public abstract KeyBuilder key(String name);

    public abstract SectionBuilder section(String name);

    public abstract ScopeBuilder comment(String line);

    public abstract ScopeBuilder inline(String comment);

    public abstract ScopeBuilder header(String line);

    public abstract FluentDocument build();

    protected KeyBuilder newKey(
            Block section,
            List<String> prefix,
            String name,
            ProgrammaticSchema schema) {
        return new KeyBuilder(this, section, prefix, name, schema);
    }
}
