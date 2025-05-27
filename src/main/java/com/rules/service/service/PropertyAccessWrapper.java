package com.rules.service.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable wrapper to make Map properties accessible as object properties in
 * SpEL
 * This allows access to both input data and output variables from previous
 * rules
 * Enhanced to support nested object access with dot notation
 */
public class PropertyAccessWrapper {
    private final Map<String, Object> properties;
    private final Map<String, Object> outputVariables;

    public PropertyAccessWrapper(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
        this.outputVariables = new HashMap<>();
    }

    /**
     * Add output variable for direct access in subsequent rules
     */
    public void addOutputVariable(String name, Object value) {
        outputVariables.put(name, value);
    }

    /**
     * This method allows SpEL to access any property dynamically with support for
     * nested paths
     */
    public Object get(String propertyName) {
        // Check output variables first (from previous rules), then input properties
        Object value = getNestedValue(outputVariables, propertyName);
        if (value != null) {
            return value;
        }
        return getNestedValue(properties, propertyName);
    }

    /**
     * Get value from nested object structure using dot notation
     * Enhanced to support array index access (e.g., "users[0].name",
     * "company.employees[1].profile.title")
     * 
     * @param source The source map to search in
     * @param path   The property path (e.g., "user.profile.name", "users[0].name")
     * @return The value at the nested path, or null if not found
     */
    private Object getNestedValue(Map<String, Object> source, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = source;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            // Check if this part contains array index notation like "users[0]"
            if (part.contains("[") && part.contains("]")) {
                current = handleArrayAccess(current, part);
            } else {
                // Regular property access
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) current;
                    current = currentMap.get(part);
                } else {
                    // If current is not a Map, we can't navigate further
                    return null;
                }
            }
        }

        return current;
    }

    /**
     * Handle array index access like "users[0]" or "employees[1]"
     * 
     * @param current The current object (should be a Map)
     * @param part    The part containing array access notation
     * @return The object at the specified array index, or null if not found/invalid
     */
    private Object handleArrayAccess(Object current, String part) {
        if (!(current instanceof Map)) {
            return null;
        }

        try {
            // Parse property name and index from "propertyName[index]"
            int openBracket = part.indexOf('[');
            int closeBracket = part.indexOf(']');

            if (openBracket == -1 || closeBracket == -1 || openBracket >= closeBracket) {
                return null;
            }

            String propertyName = part.substring(0, openBracket);
            String indexStr = part.substring(openBracket + 1, closeBracket);

            int index = Integer.parseInt(indexStr);

            @SuppressWarnings("unchecked")
            Map<String, Object> currentMap = (Map<String, Object>) current;
            Object arrayProperty = currentMap.get(propertyName);

            if (arrayProperty == null) {
                return null;
            }

            // Handle different array/list types
            if (arrayProperty instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> list = (java.util.List<Object>) arrayProperty;
                if (index >= 0 && index < list.size()) {
                    return list.get(index);
                }
            } else if (arrayProperty.getClass().isArray()) {
                Object[] array = (Object[]) arrayProperty;
                if (index >= 0 && index < array.length) {
                    return array[index];
                }
            }

            return null;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Invalid index format or parsing error
            return null;
        }
    }

    // Common property accessors for direct access
    public Object getAge() {
        return get("age");
    }

    public Object getName() {
        return get("name");
    }

    public Object getEmail() {
        return get("email");
    }

    public Object getId() {
        return get("id");
    }

    public Object getStatus() {
        return get("status");
    }

    // Generic property access using reflection-like approach
    public Object getProperty(String name) {
        return get(name);
    }

    // Allow accessing any property dynamically (this enables direct property access
    // in SpEL)
    public Object getValue(String propertyName) {
        return get(propertyName);
    }

    // Get available output variables (for debugging/logging)
    public Map<String, Object> getOutputVariables() {
        return new HashMap<>(outputVariables);
    }

    // ===== FUNCTION METHODS =====
    // These allow calling functions directly without # prefix

    /**
     * Convert string to uppercase
     */
    public String STRING_UPPERCASE(String input) {
        return SpelFunctionUtils.toUpperCase(input);
    }

    /**
     * Convert string to lowercase
     */
    public String STRING_LOWERCASE(String input) {
        return SpelFunctionUtils.toLowerCase(input);
    }

    /**
     * Get substring
     */
    public String STRING_SUBSTRING(String input, int start, int end) {
        return SpelFunctionUtils.substring(input, start, end);
    }

    /**
     * String concatenation - supports variable number of arguments
     */
    public String STRING_CONCAT(String... strings) {
        return SpelFunctionUtils.concat(strings);
    }

    @Override
    public String toString() {
        Map<String, Object> allProperties = new HashMap<>(properties);
        allProperties.putAll(outputVariables);
        return allProperties.toString();
    }
}