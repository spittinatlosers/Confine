package confine.toml;

import confine.Format;
import confine.Formats;
import confine.internal.Texts;
import confine.node.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

public final class Toml implements Format {

    @Override
    public Formats id() {
        return Formats.TOML;
    }

    @Override
    public Set<String> extensions() {
        return Collections.singleton("toml");
    }

    @Override
    public Block read(Reader reader) {
        try {
            return new Parser(Texts.read(reader)).parse();
        } catch (IOException exception) {
            throw new IllegalStateException("toml read failed", exception);
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
            writer.write(new Printer().write(root));
        } catch (IOException exception) {
            throw new IllegalStateException("toml write failed", exception);
        }
    }
}
