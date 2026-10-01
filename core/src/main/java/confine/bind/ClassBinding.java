package confine.bind;

import confine.Formats;
import confine.internal.Texts;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClassBinding {

    private final Class<?> type;
    private final boolean record;
    private final String file;
    private final String versionKey;
    private final int version;
    private final Formats format;
    private final List<String> header;
    private final List<String> comments;
    private final List<BoundMember> members;
    private final Constructor<?> constructor;

    public ClassBinding(
            Class<?> type,
            boolean record,
            String file,
            String versionKey,
            int version,
            Formats format,
            List<String> header,
            List<String> comments,
            List<BoundMember> members,
            Constructor<?> constructor) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        this.type = type;
        this.record = record;
        this.file = file == null ? "" : file;
        this.versionKey = versionKey == null || Texts.blank(versionKey)
                ? "config-version"
                : versionKey;
        this.version = version;
        this.format = format == null ? Formats.AUTO : format;
        this.header = header == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(header));
        this.comments = comments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(comments));
        this.members = members == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(members));
        this.constructor = constructor;
    }

    public Class<?> type() {
        return type;
    }

    public boolean record() {
        return record;
    }

    public String file() {
        return file;
    }

    public boolean hasFile() {
        return !Texts.blank(file);
    }

    public String versionKey() {
        return versionKey;
    }

    public int version() {
        return version;
    }

    public Formats format() {
        return format;
    }

    public List<String> header() {
        return header;
    }

    public List<String> comments() {
        return comments;
    }

    public List<BoundMember> members() {
        return members;
    }

    public Constructor<?> constructor() {
        return constructor;
    }
}
