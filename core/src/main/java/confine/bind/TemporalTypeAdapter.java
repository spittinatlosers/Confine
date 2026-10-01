package confine.bind;

import confine.node.Node;
import confine.node.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;

public final class TemporalTypeAdapter implements TypeAdapter<Object> {

    @Override
    public boolean supports(JavaType type) {
        Class<?> raw = type.raw();
        return raw == Duration.class
                || raw == Instant.class
                || raw == LocalDate.class
                || raw == LocalTime.class
                || raw == LocalDateTime.class
                || raw == OffsetDateTime.class
                || raw == ZonedDateTime.class
                || raw == Period.class;
    }

    @Override
    public Object read(Node node, MappingContext context) {
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            return null;
        }
        Value valueNode = (Value) node;
        Class<?> raw = context.current().raw();
        Object rawValue = valueNode.value();
        if (raw.isInstance(rawValue)) {
            return rawValue;
        }
        String text = String.valueOf(rawValue).trim();
        try {
            if (raw == Duration.class) {
                if (rawValue instanceof Number) {
                    Number number = (Number) rawValue;
                    return Duration.ofMillis(number.longValue());
                }
                return Duration.parse(text);
            }
            if (raw == Instant.class) {
                return Instant.parse(text);
            }
            if (raw == LocalDate.class) {
                return LocalDate.parse(text);
            }
            if (raw == LocalTime.class) {
                return LocalTime.parse(text);
            }
            if (raw == LocalDateTime.class) {
                return LocalDateTime.parse(text);
            }
            if (raw == OffsetDateTime.class) {
                return OffsetDateTime.parse(text);
            }
            if (raw == ZonedDateTime.class) {
                return ZonedDateTime.parse(text);
            }
            if (raw == Period.class) {
                return Period.parse(text);
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("expected "
                    + raw.getSimpleName()
                    + " at "
                    + context.path(), exception);
        }
        throw new IllegalStateException("unsupported temporal at " + context.path());
    }

    @Override
    public Node write(Object value, MappingContext context) {
        if (value == null) {
            return new Value(null);
        }
        return new Value(value.toString());
    }
}
