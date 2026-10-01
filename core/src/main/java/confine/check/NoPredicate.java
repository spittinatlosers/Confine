package confine.check;

public final class NoPredicate implements ConfigPredicate<Object> {

    @Override
    public boolean test(Object value) {
        return true;
    }

    @Override
    public String message() {
        return "";
    }
}
