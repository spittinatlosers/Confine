package confine.yaml;

import confine.Format;
import confine.Formats;
import confine.internal.Texts;
import confine.node.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Yaml implements Format {

    @Override
    public Formats id() {
        return Formats.YAML;
    }

    @Override
    public Set<String> extensions() {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("yaml", "yml")));
    }

    @Override
    public Block read(Reader reader) {
        try {
            return new Parser(Texts.read(reader)).parse();
        } catch (IOException exception) {
            throw new IllegalStateException("yaml read failed", exception);
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
            throw new IllegalStateException("yaml write failed", exception);
        }
    }
}
