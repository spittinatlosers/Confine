package confine.io;

@FunctionalInterface
public interface ConfigListener {

    void onEvent(ConfigEvent event);
}
