package confine.properties;

import confine.Format;
import confine.Formats;
import confine.internal.Texts;
import confine.node.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

public final class Properties implements Format {

    private final PropertiesCodec codec = new PropertiesCodec();

    @Override
    public Formats id() {
        return Formats.PROPERTIES;
    }

    @Override
    public Set<String> extensions() {
        return Collections.singleton("properties");
    }

    @Override
    public Block read(Reader reader) {
        try {
            return codec.parse(Texts.read(reader));
        } catch (IOException exception) {
            throw new IllegalStateException("properties read failed", exception);
        }
    }

    @Override
    public void write(Block root, Writer writer) {
        if (root == null) {
            throw new NullPointerException("root");
        }
        if (writer == null) {
            throw new NullPointerException("writer");
        }
        try {
            writer.write(codec.render(root));
        } catch (IOException exception) {
            throw new IllegalStateException("properties write failed", exception);
        }
    }
}
