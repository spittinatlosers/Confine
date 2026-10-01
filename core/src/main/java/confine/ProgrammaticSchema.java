package confine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProgrammaticSchema {

    private final List<KeyDefinition> keys = new ArrayList<>();

    public void add(KeyDefinition definition) {
        if (definition == null) {
            throw new NullPointerException("definition");
        }
        keys.add(definition);
    }

    public List<KeyDefinition> keys() {
        return Collections.unmodifiableList(new ArrayList<>(keys));
    }
}
