package confine.json;

import confine.Format;
import confine.Formats;
import confine.node.Block;

import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

public final class Json implements Format {

    private final Json5 delegate = new Json5(true);

    @Override
    public Formats id() {
        return Formats.JSON;
    }

    @Override
    public Set<String> extensions() {
        return Collections.singleton("json");
    }

    @Override
    public Block read(Reader reader) {
        return delegate.read(reader);
    }

    @Override
    public void write(Block root, Writer writer) {
        delegate.write(root, writer);
    }
}
