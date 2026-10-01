package confine;

import confine.node.Block;

import java.io.Reader;
import java.io.Writer;
import java.util.Set;

public interface Format {

    Formats id();

    Set<String> extensions();

    Block read(Reader reader);

    void write(Block root, Writer writer);
}
