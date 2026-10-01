package confine.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public final class Value extends Node {

    private final Object value;

    public Value(Object value) {
        this.value = normalize(value);
    }

    public Object value() {
        return value;
    }

    public boolean isNull() {
        return value == null;
    }

    @Override
    public Shape kind() {
        return Shape.VALUE;
    }

    @Override
    public Value copy() {
        Value copy = new Value(value);
        copyMetaTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Value)) {
            return false;
        }
        Value valueNode = (Value) other;
        return sameMeta(valueNode) && Objects.equals(value, valueNode.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metaHash(), value);
    }

    @Override
    public String toString() {
        return "value(" + value + ")";
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?>
                || value instanceof Collection<?>
                || value.getClass().isArray()) {
            throw new IllegalArgumentException("structured value");
        }
        if (value instanceof String
                || value instanceof Boolean
                || value instanceof BigDecimal
                || value instanceof BigInteger) {
            return value;
        }
        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double) {
            return value;
        }
        if (value instanceof Character) {
            Character character = (Character) value;
            return String.valueOf(character);
        }
        if (value instanceof Enum<?>) {
            Enum<?> enumerated = (Enum<?>) value;
            return enumerated.name();
        }
        if (value instanceof Path) {
            Path path = (Path) value;
            return path.toString();
        }
        if (value instanceof URI || value instanceof URL) {
            return value.toString();
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (value instanceof Date) {
            Date date = (Date) value;
            return date.toInstant().toString();
        }
        if (value instanceof CharSequence) {
            CharSequence sequence = (CharSequence) value;
            return sequence.toString();
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            return new BigDecimal(number.toString());
        }
        return String.valueOf(value);
    }
}
