package confine.change;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BackupService {

    public Path backup(Path source, int version) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (!Files.exists(source)) {
            return null;
        }
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String backupName;
        if (dot > 0) {
            backupName = fileName.substring(0, dot)
                    + "-backup-v"
                    + version
                    + fileName.substring(dot);
        } else {
            backupName = fileName + "-backup-v" + version;
        }
        Path target = source.resolveSibling(backupName);
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException exception) {
            throw new IllegalStateException("failed to backup " + source, exception);
        }
    }
}
