package confine;

import confine.node.Block;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Section {

    Map<String, Object> toMap();

    boolean contains(String path);

    boolean contains(String path, boolean ignoreNull);

    String currentPath();

    String name();

    Section root();

    Section parent();

    Object get(String path);

    Object get(String path, Object defaultValue);

    void set(String path, Object value);

    void remove(String path);

    Section createSection(String path);

    Section section(String path);

    boolean isSection(String path);

    String getString(String path);

    String getString(String path, String defaultValue);

    boolean isString(String path);

    int getInt(String path);

    int getInt(String path, int defaultValue);

    boolean isInt(String path);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean defaultValue);

    boolean isBoolean(String path);

    double getDouble(String path);

    double getDouble(String path, double defaultValue);

    boolean isDouble(String path);

    long getLong(String path);

    long getLong(String path, long defaultValue);

    boolean isLong(String path);

    float getFloat(String path);

    float getFloat(String path, float defaultValue);

    boolean isFloat(String path);

    byte getByte(String path);

    byte getByte(String path, byte defaultValue);

    boolean isByte(String path);

    short getShort(String path);

    short getShort(String path, short defaultValue);

    boolean isShort(String path);

    char getChar(String path);

    char getChar(String path, char defaultValue);

    boolean isChar(String path);

    <T> List<T> getList(String path, Class<T> type);

    <T> List<T> getList(String path, Class<T> type, List<T> defaultValue);

    boolean isList(String path);

    List<String> getStringList(String path);

    List<Integer> getIntegerList(String path);

    List<Boolean> getBooleanList(String path);

    List<Double> getDoubleList(String path);

    List<Float> getFloatList(String path);

    List<Long> getLongList(String path);

    List<Byte> getByteList(String path);

    List<Character> getCharacterList(String path);

    List<Short> getShortList(String path);

    <T> T get(String path, Class<T> type);

    <T> T get(String path, Class<T> type, T defaultValue);

    List<Section> getSectionList(String path);

    List<String> comments(String path);

    void comments(String path, List<String> comments);

    String inlineComment(String path);

    void inlineComment(String path, String comment);

    Set<String> keys();

    Set<String> keys(boolean deep);

    Block node();
}
