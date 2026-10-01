package confine;

import confine.check.ValidationEngine;
import confine.check.ValidationResult;
import confine.node.Block;

public final class FluentDocument {

    private final Block node;
    private final ProgrammaticSchema schema;

    public FluentDocument(Block node, ProgrammaticSchema schema) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        if (schema == null) {
            throw new NullPointerException("schema");
        }
        this.node = node;
        this.schema = schema;
    }

    public Block node() {
        return node;
    }

    public ProgrammaticSchema schema() {
        return schema;
    }

    public Section section() {
        return Memory.wrap(node);
    }

    public ValidationResult validate(ValidationEngine engine) {
        if (engine == null) {
            throw new NullPointerException("engine");
        }
        return engine.validate(node, schema.keys());
    }
}
