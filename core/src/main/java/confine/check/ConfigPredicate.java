package confine.check;

public interface ConfigPredicate<T> {

    boolean test(T value);

    default String message() {
        return "value was rejected";
    }
}
