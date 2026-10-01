package confine.json;

import confine.Format;
import confine.Formats;
import confine.internal.Texts;
import confine.node.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

public final class Json5 implements Format {

    private final boolean strict;

    public Json5() {
        this(false);
    }

    public Json5(boolean strict) {
        this.strict = strict;
    }

    @Override
    public Formats id() {
        return strict ? Formats.JSON : Formats.JSON5;
    }

    @Override
    public Set<String> extensions() {
        if (strict) {
            return Collections.singleton("json");
        }
        return Collections.singleton("json5");
    }

    @Override
    public Block read(Reader reader) {
        try {
            String text = Texts.read(reader);
            return new Json5Parser(text, strict).parse();
        } catch (IOException exception) {
            throw new IllegalStateException("json read failed", exception);
        }
    }

    @Override
    public void write(Block root, Writer writer) {
        if (writer == null) {
            throw new NullPointerException("writer");
        }
        try {
            writer.write(new Json5Writer(strict).write(root));
        } catch (IOException exception) {
            throw new IllegalStateException("json write failed", exception);
        }
    }
}
