package com.rules.service.service;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * Custom PropertyAccessor to enable dynamic property access on
 * PropertyAccessWrapper
 */
public class PropertyAccessWrapperAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] { PropertyAccessWrapper.class };
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        return target instanceof PropertyAccessWrapper;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        if (target instanceof PropertyAccessWrapper) {
            PropertyAccessWrapper wrapper = (PropertyAccessWrapper) target;
            Object value = wrapper.get(name);
            return new TypedValue(value);
        }
        throw new AccessException("Cannot read property '" + name + "' from " + target.getClass());
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return false; // We don't support writing
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue)
            throws AccessException {
        throw new AccessException("Writing not supported");
    }
}