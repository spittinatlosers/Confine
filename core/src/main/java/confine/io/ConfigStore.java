package confine.io;

import confine.Format;
import confine.node.Block;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigStore {

    private final AtomicFileWriter writer = new AtomicFileWriter();

    public Block read(Path path, Format format, Charset charset) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (format == null) {
            throw new NullPointerException("format");
        }
        try (Reader reader = Files.newBufferedReader(path, charset)) {
            Block node = format.read(reader);
            if (node == null) {
                return new Block();
            }
            return node;
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read " + path, exception);
        }
    }

    public void write(Path path, Block node, Format format, Charset charset) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        if (format == null) {
            throw new NullPointerException("format");
        }
        StringWriter buffer = new StringWriter();
        format.write(node, buffer);
        writer.write(path, buffer.toString(), charset);
    }
}
