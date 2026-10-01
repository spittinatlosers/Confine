package confine;

import confine.internal.Texts;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

public final class Registry {

    private final Map<Formats, Format> byId = new LinkedHashMap<>();
    private final Map<String, Format> byExtension = new LinkedHashMap<>();

    public Registry() {
        ClassLoader local = Registry.class.getClassLoader();
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        load(local);
        if (context != null && context != local) {
            load(context);
        }
    }

    public void register(Format format) {
        if (format == null) {
            throw new NullPointerException("format");
        }
        if (format.id() == null || format.id() == Formats.AUTO) {
            throw new IllegalStateException("format id is required");
        }
        if (byId.containsKey(format.id())) {
            return;
        }
        byId.put(format.id(), format);
        for (String extension : format.extensions()) {
            if (extension == null || Texts.blank(extension)) {
                continue;
            }
            byExtension.putIfAbsent(extension.toLowerCase(Locale.ROOT), format);
        }
    }

    public Format require(Formats id) {
        if (id == null || id == Formats.AUTO) {
            throw new IllegalStateException("format id is required");
        }
        Format format = byId.get(id);
        if (format == null) {
            throw new IllegalStateException("unsupported format " + id);
        }
        return format;
    }

    public Format resolve(Formats requested, Path path, Formats fallback) {
        if (requested != null && requested != Formats.AUTO) {
            return require(requested);
        }
        if (path != null && path.getFileName() != null) {
            String name = path.getFileName().toString();
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                Format format = byExtension.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
                if (format != null) {
                    return format;
                }
            }
        }
        Formats resolved = fallback == null || fallback == Formats.AUTO ? Formats.YAML : fallback;
        return require(resolved);
    }

    private void load(ClassLoader loader) {
        if (loader == null) {
            return;
        }
        for (Format format : ServiceLoader.load(Format.class, loader)) {
            register(format);
        }
    }
}
