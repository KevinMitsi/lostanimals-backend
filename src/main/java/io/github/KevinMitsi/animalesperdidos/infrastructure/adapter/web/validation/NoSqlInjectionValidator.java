package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.text.Normalizer;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class NoSqlInjectionValidator implements ConstraintValidator<NoSqlInjection, Object> {

    private static final int MAX_NESTING_DEPTH = 16;
    private static final Pattern SQL_SYNTAX = Pattern.compile("""
            (?:\\bunion\\s+(?:all\\s+)?select\\b)
            |(?:\\bselect\\b.{0,300}\\bfrom\\b)
            |(?:\\binsert\\s+into\\b)
            |(?:\\bupdate\\b.{0,200}\\bset\\b)
            |(?:\\bdelete\\s+from\\b)
            |(?:\\b(?:drop|alter|truncate)\\s+(?:table|database|schema)\\b)
            |(?:\\bcreate\\s+(?:table|database|schema|user|role)\\b)
            |(?:\\b(?:grant|revoke)\\b.{0,100}\\bon\\b)
            |(?:;\\s*(?:select|insert|update|delete|drop|alter|truncate|create|grant|revoke|call|execute)\\b)
            |(?:['"]\\s*(?:or|and)\\s+(?:['"]?\\w+['"]?|\\d+)\\s*=\\s*(?:['"]?\\w+['"]?|\\d+))
            |(?:\\b(?:or|and)\\s+\\d+\\s*=\\s*\\d+\\s*(?:--|\\x23|/\\*))
            |(?:\\b(?:pg_sleep|sleep|benchmark)\\s*\\()
            |(?:\\bcopy\\b.{0,200}\\bprogram\\b)
            """, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL | Pattern.COMMENTS);

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return isSafe(value, visited, 0);
    }

    private boolean isSafe(Object value, Set<Object> visited, int depth) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence text) {
            String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
            return !SQL_SYNTAX.matcher(normalized).find();
        }
        if (depth >= MAX_NESTING_DEPTH || !visited.add(value)) {
            return true;
        }
        if (value instanceof Optional<?> optional) {
            return optional.isEmpty() || isSafe(optional.get(), visited, depth + 1);
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                if (!isSafe(element, visited, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                if (!isSafe(entry.getKey(), visited, depth + 1)
                        || !isSafe(entry.getValue(), visited, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                if (!isSafe(Array.get(value, index), visited, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (!value.getClass().isRecord()) {
            return true;
        }
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            try {
                if (!isSafe(component.getAccessor().invoke(value), visited, depth + 1)) {
                    return false;
                }
            } catch (ReflectiveOperationException exception) {
                throw new ValidationException("Could not inspect " + value.getClass().getName(), exception);
            }
        }
        return true;
    }
}
