package confine.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public final class AtomicFileWriter {

    public void write(Path path, String content, Charset charset) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (content == null) {
            throw new NullPointerException("content");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        Path parent = path.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = path.resolveSibling(path.getFileName().toString()
                    + ".tmp-"
                    + UUID.randomUUID());
            try {
                try (FileChannel channel = FileChannel.open(
                        temp,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(charset));
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                    channel.force(true);
                }
                try {
                    Files.move(
                            temp,
                            path,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException suppressed) {
                    exception.addSuppressed(suppressed);
                }
                throw exception;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write " + path, exception);
        }
    }
}
