package confine;

import confine.internal.Texts;

import java.util.Locale;

public enum Formats {
    AUTO,
    YAML,
    TOML,
    JSON5,
    JSON,
    HOCON,
    PROPERTIES;

    public static Formats byExtension(String fileName) {
        if (fileName == null || Texts.blank(fileName)) {
            return AUTO;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        int dot = normalized.lastIndexOf('.');
        if (dot < 0 || dot == normalized.length() - 1) {
            return AUTO;
        }
        String extension = normalized.substring(dot + 1);
        switch (extension) {
                case "yml":
                case "yaml":
                    return YAML;
                case "toml":
                    return TOML;
                case "json5":
                    return JSON5;
                case "json":
                    return JSON;
                case "conf":
                case "hocon":
                    return HOCON;
                case "properties":
                    return PROPERTIES;
                default:
                    return AUTO;
            }
    }
}
