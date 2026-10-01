# Confine

Confine reads and writes a configuration file as a tree of sections and values. YAML, TOML, JSON, JSON5, HOCON, and Properties share that tree, so the same calls work for every format.

`Formats.AUTO` picks the parser from the file extension. A file with no known extension uses the default format, YAML, unless `defaultFormat` changes it.

Two ways to use a file:

- `open` returns a `Section`. Read and write keys, lists, and nested sections, then `save`.
- `load(Class)` maps the file onto fields with `@Config`, `@Key`, and `@Validate`. `save` writes the object back.

Comments, classpath defaults, versioned migrations, validation, file watch, and async load and save all use that same tree.

## Formats

| Format | Extensions | Comments |
| --- | --- | --- |
| YAML | `.yaml`, `.yml` | kept |
| TOML | `.toml` | kept |
| JSON | `.json` | dropped on write |
| JSON5 | `.json5` | kept |
| HOCON | `.conf`, `.hocon` | kept |
| Properties | `.properties` | kept |

## Open a file

```java
Path directory = Paths.get("config");
try (Confine confine = Confine.builder().directory(directory).build()) {
    Section section = confine.open(directory.resolve("app.yaml"));
    String name = section.getString("name");
    int port = section.getInt("port");
    String host = section.section("server").getString("host");
    section.set("port", 25565);
    section.createSection("server").set("host", "localhost");
    confine.save(section, directory.resolve("app.yaml"));
}
```

`open` without a format uses the extension. Pass `Formats.JSON`, `Formats.JSON5`, `Formats.TOML`, `Formats.HOCON`, `Formats.PROPERTIES`, or `Formats.YAML` when the extension is wrong or missing.

`section(path)` returns the existing section, or `null` when that path is not a section. `createSection(path)` creates the missing sections and returns the last one.

Paths use dots: `server.host`. A list of sections is `rows[0].name` in the file and `getSectionList("rows")` in code.

Closing `Confine` is idempotent. Calls after `close` throw `IllegalStateException`.

## Read and write values

Every getter has a form with a default. The form without a default returns `0`, `false`, `'\0'`, or `null` when the value is missing.

```java
section.getString("name", "demo");
section.getInt("port", 8080);
section.getLong("count", 0L);
section.getDouble("ratio", 1.0D);
section.getBoolean("enabled", true);
section.getChar("mark", 'z');
section.getByte("small", (byte) 0);
section.getShort("mid", (short) 0);
section.getFloat("grade", 0F);

section.getStringList("items");
section.getIntegerList("scores");
section.getBooleanList("flags");
section.getDoubleList("ratios");
section.getLongList("counts");
section.getFloatList("grades");
section.getByteList("bytes");
section.getShortList("shorts");
section.getCharacterList("marks");
section.getList("items", String.class, Arrays.asList("alpha"));

section.contains("name");
section.contains("empty", true);
section.isString("name");
section.isInt("port");
section.isSection("server");
section.isList("items");
section.remove("name");
section.keys();
section.keys(true);
section.toMap();
```

`contains(path, true)` treats a null value as absent.

Comments:

```java
section.comments("name", Arrays.asList("display name"));
section.inlineComment("name", "primary");
List<String> lines = section.comments("name");
String inline = section.inlineComment("name");
```

JSON does not keep comments. JSON5, YAML, TOML, HOCON, and Properties do, to the extent that format allows.

A section can be built in memory and saved:

```java
Memory memory = Memory.create();
memory.set("name", "demo");
memory.set("items", Arrays.asList("alpha", "beta"));
memory.createSection("server").set("host", "localhost");
confine.save(memory, file, Formats.YAML);
```

`Memory.wrap(block)` wraps an existing node tree.

## Map a class

```java
@Config(file = "app.yaml", version = 1, format = Formats.YAML)
@Header({"generated"})
public class Settings {
    @Key("name")
    @Comment({"display name"})
    public String name = "demo";

    @Key("port")
    @Validate(min = 1, max = 65535, message = "port is out of range")
    public int port = 8080;

    public Mode mode = Mode.ON;

    public List<String> items = new ArrayList<String>(Arrays.asList("alpha"));

    @Nested
    @Key("server")
    public Server server = new Server();

    public Map<String, String> labels = new LinkedHashMap<String, String>();

    public Optional<String> note = Optional.empty();

    @Ignore
    public String secret = "hidden";
}

public class Server {
    @Key("host")
    public String host = "localhost";
}
```

```java
try (Confine confine = Confine.builder().directory(directory).build()) {
    Settings settings = confine.load(Settings.class);
    settings.port = 25565;
    confine.save(settings);
}
```

`load(Class)` uses `@Config.file` inside `directory`. `load(Class, path)` uses that path instead. `save(Object)` writes the file from `@Config`. `save(Object, path)` writes that path.

`@Key` sets the path. Without it, the field name is the key. `@Nested` maps the field as a section. `@Ignore` skips the field on read and write; the Java initializer stays. `@Header` is written as comments on the root. `@Comment` is written on that key.

Supported field types: `String`, primitive numbers and their wrappers, `boolean`, `char`, `BigDecimal`, `BigInteger`, enums, `List`, arrays, `Map`, nested objects, `Optional`, `UUID`, `Path`, `URI`, `URL`, `Duration`, `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, and `Period`.

`format = Formats.AUTO` selects the parser from the file name.

If the file does not exist, Confine creates it from the field initializers when `saveOnChange` is true, which is the default.

## Validation

On a field:

```java
@Validate(min = 1, max = 65535)
public int port;

@Validate(regex = "[a-z]+")
public String code;

@Validate(notNull = true)
public String title;

@Validate(notBlank = true)
public String name;

@Validate(oneOf = {"on", "off"})
public String mode;

@Validate(predicate = OkOnly.class, message = "flag must be ok")
public String flag;
```

`OkOnly` must be a public class with a public no-argument constructor:

```java
public final class OkOnly implements ConfigPredicate<String> {
    public boolean test(String value) {
        return "ok".equals(value);
    }
}
```

Invalid data throws `IllegalStateException` when `failOnValidation` is true. Set `failOnValidation(false)` to keep the loaded object and publish `VALIDATION_FAILED`.

## Fluent document

```java
FluentDocument document = Confine.schema()
        .header("service")
        .version(3)
        .key("name").comment("display name").string("demo").inline("primary").notBlank().end()
        .key("port").integer(8080).min(1).max(65535).end()
        .key("enabled").bool(true).end()
        .key("items").array("alpha", "beta").end()
        .section("server").key("host").string("localhost").end()
        .build();

document.validate(new ValidationEngine()).throwIfInvalid();
confine.save(document, file, Formats.YAML);
```

Key methods: `string`, `bool`, `integer`, `longValue`, `decimal`, `number`, `nil`, `list`, `array`. Checks: `min`, `max`, `regex`, `notNull`, `notBlank`, `oneOf`, `predicate`. `save(FluentDocument, path, format)` validates before it writes.

## Migration

The version is an integer under `config-version`, unless `versionKey` or `@Config.versionKey` says otherwise. `@Config.version` is the target version.

Annotation rules use `from -> to`:

```java
@Config(file = "server.yaml", version = 2)
@Migration(from = 1, to = 2, rename = {"old-name->name"}, move = {"legacy->stats.count"}, remove = {"drop"})
public class ServerSettings {
    public String name = "default";
    public int port = 8080;
}
```

When the file version is lower than the target, Confine copies the file to `server-backup-v1.yaml` and then applies the rules. `backupBeforeMigration(false)` skips the copy. A file that is already at or above the target version is not migrated.

The same rules from code. `copy` and `transform` exist only on the builder:

```java
confine.migrations()
        .type(Shop.class)
        .file("shop.yaml")
        .from(1)
        .to(2)
        .rename("title", "name")
        .move("legacy", "stats.count")
        .copy("title", "alias")
        .remove("drop")
        .transform("name", new Function<Object, Object>() {
            public Object apply(Object value) {
                return String.valueOf(value).toLowerCase();
            }
        })
        .end()
        .register();
```

Call `register()` before `load`.

## Defaults from the classpath

Put a file with the same name as `@Config.file` on the classpath, for example `src/main/resources/app.yaml`. On load, missing keys are filled from that file. User values already in the real file stay.

`fillDefaults(false)` leaves the file as it is. `resourceRoot("defaults")` looks in `defaults/app.yaml` first, then `app.yaml`.

`MergePolicy`:

| Value | Behavior |
| --- | --- |
| `FILL_MISSING` | Copy missing keys from the defaults. |
| `FILL_MISSING_WITH_COMMENTS` | Same, and copy comments onto existing keys that have none. This is the default. |
| `PREFER_DEFAULTS` | Defaults replace the user file where both define a value. |

## Watch, reload, events

```java
try (Confine confine = Confine.builder()
        .directory(directory)
        .watchEnabled(true)
        .autoReload(true)
        .watchDebounceMillis(200L)
        .build()) {
    Subscription subscription = confine.listen(new ConfigListener() {
        public void onEvent(ConfigEvent event) {
            if (event.type() == ConfigEventType.RELOADED) {
                Settings latest = confine.latest(Settings.class);
            }
        }
    });
    confine.load(Settings.class);
    subscription.unsubscribe();
}
```

`watchEnabled(true)` watches files passed to `open`, `load`, and `watch(path)`. `autoReload(false)` publishes `CHANGED` and does not load again. `autoReload(true)` loads again and publishes `RELOADED`.

Event types: `LOADED`, `SAVED`, `MIGRATED`, `RELOADED`, `CHANGED`, `VALIDATION_FAILED`, `BACKUP_CREATED`, `FAILED`.

`latest(Class)` returns the last instance tracked for that class in this `Confine`, or `null`.

## Async

```java
Settings settings = confine.loadAsync(Settings.class).get(5, TimeUnit.SECONDS);
confine.saveAsync(settings).get(5, TimeUnit.SECONDS);
```

`ioThreads` sets the pool size.

## Custom type

```java
public final class LabelAdapter implements TypeAdapter<Label> {
    public boolean supports(JavaType type) {
        return type.raw() == Label.class;
    }

    public Label read(Node node, MappingContext context) {
        return new Label(String.valueOf(((Value) node).value()));
    }

    public Node write(Label value, MappingContext context) {
        return new Value(value.text);
    }
}
```

On a field:

```java
@Adapter(LabelAdapter.class)
public Label label;
```

The adapter class needs a public no-argument constructor. `builder.adapter(new LabelAdapter())` registers one for every matching type.

## Parser without Confine

```java
Yaml yaml = new Yaml();
Block block = yaml.read(new StringReader("name: demo\n"));
StringWriter writer = new StringWriter();
yaml.write(block, writer);
```

The same shape exists on `Toml`, `Json`, `Json5`, `Hocon`, and `Properties`. `read` returns a `Block`. `Memory.wrap` turns that block into a `Section`.

## Builder

`directory` is the folder used for relative `@Config` files. `charset` is the file encoding. `versionKey` is the key that stores the migration version. `saveOnChange` writes the file when load creates it, fills defaults, or migrates it. `save` always writes. `cacheMaximumSize` limits cached documents and class bindings. `classLoader` is used to find classpath defaults.

Suggestions or something that does not work? Contact [@spittinatlosers](https://t.me/spittinatlosers).
