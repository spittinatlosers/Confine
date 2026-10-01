package confine.io;

import confine.Format;
import confine.internal.Texts;
import confine.node.Block;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ResourceReader {

    private final ClassLoader classLoader;
    private final Charset charset;

    public ResourceReader(ClassLoader classLoader, Charset charset) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.classLoader = classLoader;
        this.charset = charset;
    }

    public Optional<Block> read(String resourceRoot, String fileName, Format format) {
        if (fileName == null || Texts.blank(fileName)) {
            throw new IllegalArgumentException("fileName");
        }
        if (format == null) {
            throw new NullPointerException("format");
        }
        for (String candidate : candidates(resourceRoot, fileName)) {
            Optional<String> text = readText(candidate);
            if (!text.isPresent()) {
                continue;
            }
            return Optional.of(format.read(new StringReader(text.get())));
        }
        return Optional.empty();
    }

    private List<String> candidates(String resourceRoot, String fileName) {
        List<String> candidates = new ArrayList<>();
        String normalized = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        if (resourceRoot != null && !Texts.blank(resourceRoot)) {
            String root = resourceRoot.endsWith("/")
                    ? resourceRoot.substring(0, resourceRoot.length() - 1)
                    : resourceRoot;
            if (root.startsWith("/")) {
                root = root.substring(1);
            }
            candidates.add(root + "/" + normalized);
        }
        candidates.add(normalized);
        return candidates;
    }

    private Optional<String> readText(String location) {
        InputStream stream = classLoader.getResourceAsStream(location);
        if (stream == null) {
            return Optional.empty();
        }
        try (InputStream input = stream; Reader reader = new InputStreamReader(input, charset)) {
            return Optional.of(Texts.read(reader));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read resource " + location, exception);
        }
    }
}
