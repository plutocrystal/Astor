package net.dev.astor.event;

import net.dev.astor.event.types.Priority;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTarget {
    byte value() default Priority.MEDIUM;
}