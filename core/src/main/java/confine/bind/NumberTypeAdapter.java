package confine.bind;

import confine.internal.Scalars;
import confine.node.Node;
import confine.node.Value;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class NumberTypeAdapter implements TypeAdapter<Number> {

    @Override
    public boolean supports(JavaType type) {
        Class<?> raw = type.raw();
        return raw == byte.class
                || raw == short.class
                || raw == int.class
                || raw == long.class
                || raw == float.class
                || raw == double.class
                || raw == Byte.class
                || raw == Short.class
                || raw == Integer.class
                || raw == Long.class
                || raw == Float.class
                || raw == Double.class
                || raw == BigInteger.class
                || raw == BigDecimal.class
                || raw == Number.class;
    }

    @Override
    public Number read(Node node, MappingContext context) {
        Class<?> raw = context.current() == null ? Number.class : context.current().raw();
        if (!(node instanceof Value) || ((Value) node).value() == null) {
            if (raw.isPrimitive()) {
                return (Number) Scalars.primitiveDefault(raw);
            }
            return null;
        }
        Value valueNode = (Value) node;
        Object value = valueNode.value();
        if (raw == byte.class || raw == Byte.class) {
            Byte parsed = Scalars.tryByte(value);
            if (parsed == null) {
                throw new IllegalStateException("expected byte at " + context.path());
            }
            return parsed;
        }
        if (raw == short.class || raw == Short.class) {
            Short parsed = Scalars.tryShort(value);
            if (parsed == null) {
                throw new IllegalStateException("expected short at " + context.path());
            }
            return parsed;
        }
        if (raw == int.class || raw == Integer.class) {
            Integer parsed = Scalars.tryInt(value);
            if (parsed == null) {
                throw new IllegalStateException("expected int at " + context.path());
            }
            return parsed;
        }
        if (raw == long.class || raw == Long.class) {
            Long parsed = Scalars.tryLong(value);
            if (parsed == null) {
                throw new IllegalStateException("expected long at " + context.path());
            }
            return parsed;
        }
        if (raw == float.class || raw == Float.class) {
            Float parsed = Scalars.tryFloat(value);
            if (parsed == null) {
                throw new IllegalStateException("expected float at " + context.path());
            }
            return parsed;
        }
        if (raw == double.class || raw == Double.class) {
            Double parsed = Scalars.tryDouble(value);
            if (parsed == null) {
                throw new IllegalStateException("expected double at " + context.path());
            }
            return parsed;
        }
        if (raw == BigInteger.class) {
            try {
                if (value instanceof BigInteger) {
                    return (BigInteger) value;
                }
                if (value instanceof BigDecimal) {
                    return ((BigDecimal) value).toBigIntegerExact();
                }
                return new BigInteger(String.valueOf(value).trim());
            } catch (RuntimeException exception) {
                throw new IllegalStateException("expected integer at " + context.path(), exception);
            }
        }
        if (raw == BigDecimal.class) {
            try {
                if (value instanceof BigDecimal) {
                    return (BigDecimal) value;
                }
                return new BigDecimal(String.valueOf(value).trim());
            } catch (RuntimeException exception) {
                throw new IllegalStateException("expected decimal at " + context.path(), exception);
            }
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        Double parsed = Scalars.tryDouble(value);
        if (parsed == null) {
            throw new IllegalStateException("expected number at " + context.path());
        }
        return parsed;
    }

    @Override
    public Node write(Number value, MappingContext context) {
        return new Value(value);
    }
}
