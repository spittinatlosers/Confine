package confine.internal;

public final class Require {

    private Require() {
    }

    public static <T> T nonNull(T value, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        return value;
    }

    public static String nonBlank(String value, String name) {
        if (value == null || Texts.blank(value)) {
            throw new IllegalArgumentException(name);
        }
        return value;
    }

    public static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name);
        }
        return value;
    }

    public static long notNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name);
        }
        return value;
    }
}
