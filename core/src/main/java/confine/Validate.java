package confine;

import confine.check.ConfigPredicate;
import confine.check.NoPredicate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Validate {

    double min() default Double.NaN;

    double max() default Double.NaN;

    String regex() default "";

    boolean notNull() default false;

    boolean notBlank() default false;

    String[] oneOf() default {};

    String message() default "";

    Class<? extends ConfigPredicate<?>> predicate() default NoPredicate.class;
}
