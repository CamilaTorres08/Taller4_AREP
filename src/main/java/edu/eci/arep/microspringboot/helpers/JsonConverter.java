package edu.eci.arep.microspringboot.helpers;

import java.lang.reflect.Field;
import java.util.List;

public class JsonConverter {
    /**
     * Serializes an object into its JSON string representation.
     * @param obj object to convert
     * @return JSON string representation
     */
    public static String toJson(Object obj) {
        if(obj instanceof String) return quote((String) obj);
        if(obj instanceof Number) return obj.toString();
        if(obj instanceof List<?>) {
            return arrayToJson(obj);
        }
        return objectToJson(obj);
    }

    /**
     * Converts a list into a JSON array string.
     * @param array the list to convert
     * @return JSON array as a string
     */
    private static String arrayToJson(Object array) {
        StringBuilder sb = new StringBuilder("[");
        List<?> list = (List<?>) array;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            sb.append(toJson(item));
            if(i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Serializes a generic Java object into a JSON object string.
     * @param obj the object to serialize
     * @return JSON object as a string
     */
    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (!first) sb.append(",");
                sb.append("\"").append(field.getName()).append("\":");
                sb.append(toJson(value));
                first = false;
            } catch (IllegalAccessException e) {}
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escapes and wraps a string in double quotes to produce a valid JSON string value.
     * @param s the input string to escape
     * @return the JSON-safe string wrapped in double quotes
     */
    private static String quote(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
