package confine.check;

import confine.internal.Texts;

import java.util.Objects;

public final class ValidationError {

    private final String path;
    private final String message;

    public ValidationError(String path, String message) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (message == null || Texts.blank(message)) {
            throw new IllegalArgumentException("message");
        }
        this.path = path;
        this.message = message;
    }

    public String path() {
        return path;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidationError)) {
            return false;
        }
        ValidationError error = (ValidationError) other;
        return path.equals(error.path) && message.equals(error.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, message);
    }

    @Override
    public String toString() {
        return path + ": " + message;
    }
}
