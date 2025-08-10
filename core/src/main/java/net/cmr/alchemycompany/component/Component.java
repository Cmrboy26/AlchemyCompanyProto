package net.cmr.alchemycompany.component;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public abstract class Component implements Serializable {
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("[");
        Field[] fields = getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            try {
                sb.append(fields[i].getName()).append(":").append(fields[i].get(this));
            } catch (IllegalAccessException e) {
                sb.append(fields[i].getName()).append(":<inaccessible>");
            }
            if (i < fields.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static <K extends Enum<K>, V> HashMap<K, V> readMap(Class<K> keyClass, Class<V> valueClass, JsonValue jsonData, Json json) {
        HashMap<K, V> result = readMap(keyClass, valueClass, null, jsonData, json);
        if (result == null) {
            throw new RuntimeException("No value found.");
        }
        return result;
    }

    public static <K extends Enum<K>, V> HashMap<K, V> readMap(Class<K> keyClass, Class<V> valueClass, HashMap<K, V> defaultValue, JsonValue jsonData, Json json) {
        HashMap<K, V> result = new HashMap<>();
        if (jsonData == null || jsonData.size == 0) {
            return defaultValue;
        }
        for (JsonValue child = jsonData.child; child != null; child = child.next) {
            K key = Enum.valueOf(keyClass, child.name);
            V value = json.readValue(valueClass, child);
            result.put(key, value);
        }
        return result;
    }

}
