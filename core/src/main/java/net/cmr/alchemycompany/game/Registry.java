package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;

import net.cmr.alchemycompany.ecs.Entity;

import com.badlogic.gdx.utils.JsonValue;

public class Registry {

    private static Registry instance = null;

    public static Registry getInstance() {
        if (instance == null) {
            instance = new Registry();
            instance.registerAll();
        }
        return instance;
    }

    // You cannot directly reference the type parameter from the outer map in the inner map.
    // Instead, you can use Map<Class<? extends Serializable>, Map<String, Serializable>>.
    // If you want type safety, you need to use a generic class.
    public Map<Class<? extends Serializable>, Map<String, Serializable>> register;

    private Registry() {
        System.out.println("INITIALIZING REGISTRY.");
        register = new HashMap<>();
    }

    private void registerAll() {
        registerObjects(Resource.class, Gdx.files.internal("gamedata/resources.json"));
        registerObjects(Recipe.class, Gdx.files.internal("gamedata/recipes.json"));
    }

    private void registerObjects(Class<? extends Serializable> clazz, FileHandle jsonFile) {
        try {
            Json json = new Json();
            JsonValue root = json.fromJson(null, jsonFile);
            JsonValue serializableArray = root.get("registry");
            register.putIfAbsent(clazz, new HashMap<>());

            for (JsonValue serializableData = serializableArray.child; serializableData != null; serializableData = serializableData.next) {
                Serializable serializableObj = clazz.newInstance();
                serializableObj.read(json, serializableData);
                // Register
                System.out.println("Registering... "+serializableData.getString("id"));
                register.get(clazz).put(serializableData.getString("id"), serializableObj);
            }
            
            System.out.println("Loaded " + register.get(clazz).size() + " objects from "+jsonFile.name());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T extends Serializable> Map<String, T> getRegistry(Class<T> clazz) {
        return (Map<String, T>) register.get(clazz);
    }

    // Helper methods

    public static Map<String, Resource> getResourceRegistry() {
        return getInstance().getRegistry(Resource.class);
    }
    public enum ResourceFilter {
        ALL,
        OMIT_PER_TURN,
        PER_TURN
    }
    public static Map<String, Resource> getResourceValues(ResourceFilter filter) {
        Map<String, Resource> map = new HashMap<>();
        for (Entry<String, Resource> entry : getResourceRegistry().entrySet()) {
            if (filter == ResourceFilter.OMIT_PER_TURN) {
                if (entry.getValue().isPerTurnResource()) {
                    continue;
                }
            } else if (filter == ResourceFilter.PER_TURN) {
                if (!entry.getValue().isPerTurnResource()) {
                    continue;
                }
            }
            map.put(entry.getKey(), entry.getValue());
        }
        return map; 
    }

}
