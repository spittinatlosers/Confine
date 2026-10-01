package confine.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

public final class Scalars {

    private Scalars() {
    }

    public static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    public static String asText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof String) {
            String text = (String) value;
            return text;
        }
        return String.valueOf(value);
    }

    public static Integer tryInt(Object value) {
        Number number = integral(value);
        if (number == null) {
            return null;
        }
        if (number instanceof BigInteger) {
            BigInteger big = (BigInteger) number;
            if (big.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                    || big.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                return null;
            }
            return big.intValue();
        }
        long raw = number.longValue();
        if (raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE) {
            return null;
        }
        return (int) raw;
    }

    public static int toInt(Object value, int fallback) {
        Integer parsed = tryInt(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Long tryLong(Object value) {
        Number number = integral(value);
        if (number == null) {
            return null;
        }
        if (number instanceof BigInteger
                && (((BigInteger) number).compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0
                || ((BigInteger) number).compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0)) {
            return null;
        }
        return number.longValue();
    }

    public static long toLong(Object value, long fallback) {
        Long parsed = tryLong(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Double tryDouble(Object value) {
        if (value instanceof Number && !(value instanceof BigInteger)) {
            Number number = (Number) value;
            return number.doubleValue();
        }
        if (value instanceof String) {
            String text = (String) value;
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    public static double toDouble(Object value, double fallback) {
        Double parsed = tryDouble(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Float tryFloat(Object value) {
        Double parsed = tryDouble(value);
        if (parsed == null) {
            return null;
        }
        return parsed.floatValue();
    }

    public static float toFloat(Object value, float fallback) {
        Float parsed = tryFloat(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Short tryShort(Object value) {
        Long parsed = tryLong(value);
        if (parsed == null || parsed < Short.MIN_VALUE || parsed > Short.MAX_VALUE) {
            return null;
        }
        return parsed.shortValue();
    }

    public static short toShort(Object value, short fallback) {
        Short parsed = tryShort(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Byte tryByte(Object value) {
        Long parsed = tryLong(value);
        if (parsed == null || parsed < Byte.MIN_VALUE || parsed > Byte.MAX_VALUE) {
            return null;
        }
        return parsed.byteValue();
    }

    public static byte toByte(Object value, byte fallback) {
        Byte parsed = tryByte(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Boolean tryBoolean(Object value) {
        if (value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            return bool;
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            double raw = number.doubleValue();
            if (raw == 1D) {
                return true;
            }
            if (raw == 0D) {
                return false;
            }
            return null;
        }
        if (value instanceof String) {
            String text = (String) value;
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("true")
                    || normalized.equals("yes")
                    || normalized.equals("on")
                    || normalized.equals("y")
                    || normalized.equals("1")) {
                return true;
            }
            if (normalized.equals("false")
                    || normalized.equals("no")
                    || normalized.equals("off")
                    || normalized.equals("n")
                    || normalized.equals("0")) {
                return false;
            }
        }
        return null;
    }

    public static boolean toBoolean(Object value, boolean fallback) {
        Boolean parsed = tryBoolean(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Character tryChar(Object value) {
        if (value instanceof Character) {
            Character character = (Character) value;
            return character;
        }
        if (value instanceof String && ((String) value).length() == 1) {
            String text = (String) value;
            return text.charAt(0);
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            int raw = number.intValue();
            if (raw >= Character.MIN_VALUE && raw <= Character.MAX_VALUE) {
                return (char) raw;
            }
        }
        return null;
    }

    public static char toChar(Object value, char fallback) {
        Character parsed = tryChar(value);
        if (parsed == null) {
            return fallback;
        }
        return parsed;
    }

    public static Coercion tryCoerce(Object value, Class<?> type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        Class<?> target = wrap(type);
        if (value == null) {
            if (type.isPrimitive()) {
                return Coercion.ok(primitiveDefault(type));
            }
            return Coercion.ok(null);
        }
        if (target.isInstance(value)) {
            return Coercion.ok(value);
        }
        if (target == String.class) {
            return Coercion.ok(String.valueOf(value));
        }
        if (target == Integer.class) {
            Integer parsed = tryInt(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Long.class) {
            Long parsed = tryLong(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Double.class) {
            Double parsed = tryDouble(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Float.class) {
            Float parsed = tryFloat(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Short.class) {
            Short parsed = tryShort(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Byte.class) {
            Byte parsed = tryByte(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Boolean.class) {
            Boolean parsed = tryBoolean(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target == Character.class) {
            Character parsed = tryChar(value);
            return parsed == null ? Coercion.fail() : Coercion.ok(parsed);
        }
        if (target.isEnum()) {
            String name = value instanceof Enum<?>
                    ? ((Enum<?>) value).name()
                    : String.valueOf(value).trim();
            Object[] constants = target.getEnumConstants();
            for (Object constant : constants) {
                Enum<?> enumerated = (Enum<?>) constant;
                if (enumerated.name().equals(name) || enumerated.name().equalsIgnoreCase(name)) {
                    return Coercion.ok(enumerated);
                }
            }
            return Coercion.fail();
        }
        return Coercion.fail();
    }

    public static Class<?> wrap(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    public static boolean isIntegral(Object value) {
        return value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigInteger;
    }

    public static boolean isDecimal(Object value) {
        return value instanceof Float || value instanceof Double || value instanceof BigDecimal;
    }

    private static Number integral(Object value) {
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) value;
            try {
                return decimal.toBigIntegerExact();
            } catch (ArithmeticException exception) {
                return null;
            }
        }
        if (value instanceof BigInteger) {
            BigInteger big = (BigInteger) value;
            return big;
        }
        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return (Number) value;
        }
        if (value instanceof Float || value instanceof Double) {
            double raw = ((Number) value).doubleValue();
            if (Double.isNaN(raw) || Double.isInfinite(raw) || raw != Math.rint(raw)) {
                return null;
            }
            return (long) raw;
        }
        if (value instanceof String) {
            String text = (String) value;
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                if (trimmed.contains(".") || trimmed.contains("e") || trimmed.contains("E")) {
                    return integral(new BigDecimal(trimmed));
                }
                return new BigInteger(trimmed);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    public static final class Coercion {

        private final boolean success;
        private final Object value;

        private Coercion(boolean success, Object value) {
            this.success = success;
            this.value = value;
        }

        public static Coercion ok(Object value) {
            return new Coercion(true, value);
        }

        public static Coercion fail() {
            return new Coercion(false, null);
        }

        public boolean success() {
            return success;
        }

        public Object value() {
            return value;
        }
    }
}
