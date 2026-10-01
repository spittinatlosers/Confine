package confine.check;

public interface Validator<T> {

    ValidationError validate(String path, T value);
}
