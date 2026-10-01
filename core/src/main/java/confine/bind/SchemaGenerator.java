package confine.bind;

import confine.node.Block;

public final class SchemaGenerator {

    private final ConfigMapper mapper;

    public SchemaGenerator(ConfigMapper mapper) {
        if (mapper == null) {
            throw new NullPointerException("mapper");
        }
        this.mapper = mapper;
    }

    public Block generate(Class<?> type) {
        return mapper.schema(type);
    }
}
