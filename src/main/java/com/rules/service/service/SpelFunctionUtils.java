package com.rules.service.service;

/**
 * Utility class containing SpEL functions for rule evaluation
 */
public class SpelFunctionUtils {

    /**
     * Convert string to uppercase
     */
    public static String toUpperCase(String input) {
        return input != null ? input.toUpperCase() : null;
    }

    /**
     * Convert string to lowercase
     */
    public static String toLowerCase(String input) {
        return input != null ? input.toLowerCase() : null;
    }

    /**
     * Get substring
     */
    public static String substring(String input, int start, int end) {
        if (input == null)
            return null;
        try {
            return input.substring(start, end);
        } catch (StringIndexOutOfBoundsException e) {
            return input;
        }
    }

    /**
     * String concatenation - supports variable number of arguments
     */
    public static String concat(String... strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String str : strings) {
            result.append(str != null ? str : "");
        }
        return result.toString();
    }

    /**
     * String length
     */
    public static int length(String input) {
        return input != null ? input.length() : 0;
    }

    /**
     * String contains check
     */
    public static boolean contains(String input, String searchString) {
        return input != null && searchString != null && input.contains(searchString);
    }

    /**
     * String starts with check
     */
    public static boolean startsWith(String input, String prefix) {
        return input != null && prefix != null && input.startsWith(prefix);
    }

    /**
     * String ends with check
     */
    public static boolean endsWith(String input, String suffix) {
        return input != null && suffix != null && input.endsWith(suffix);
    }

    /**
     * String trim
     */
    public static String trim(String input) {
        return input != null ? input.trim() : null;
    }
}