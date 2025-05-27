package com.rules.service.service;

import java.util.Map;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Custom PropertyAccessor to handle nested Map access directly
 * This allows SpEL to navigate nested Map structures using dot notation
 */
public class NestedMapPropertyAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] { Map.class };
    }

    @Override
    public boolean canRead(@NonNull EvaluationContext context, @Nullable Object target, @NonNull String name)
            throws AccessException {
        return target instanceof Map;
    }

    @Override
    @NonNull
    public TypedValue read(@NonNull EvaluationContext context, @Nullable Object target, @NonNull String name)
            throws AccessException {
        if (target instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) target;
            Object value = map.get(name);
            return new TypedValue(value);
        }
        throw new AccessException(
                "Cannot read property '" + name + "' from " + (target != null ? target.getClass() : "null"));
    }

    @Override
    public boolean canWrite(@NonNull EvaluationContext context, @Nullable Object target, @NonNull String name)
            throws AccessException {
        return false; // We don't support writing
    }

    @Override
    public void write(@NonNull EvaluationContext context, @Nullable Object target, @NonNull String name,
            @Nullable Object newValue)
            throws AccessException {
        throw new AccessException("Writing not supported");
    }
}