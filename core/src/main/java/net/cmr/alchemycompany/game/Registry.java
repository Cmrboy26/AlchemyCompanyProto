package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Registry {

    private static Registry instance = null;
    private static final boolean TEST_OBJECTS = true;

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
        registerObjects(Technology.class, Gdx.files.internal("gamedata/technologies.json"));
        if (TEST_OBJECTS) {
            testRegistry();
        }
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
            throw new Error("Failed to parse registry file \""+jsonFile.nameWithoutExtension()+"\"", e);
        }
    }

    public <T extends Serializable> Map<String, T> getRegistry(Class<T> clazz) {
        return (Map<String, T>) register.get(clazz);
    }

    private void testRegistry() {
        for (Recipe recipe : getRegistry(Recipe.class).values()) {
            Set<String> testResources = new HashSet<>();
            testResources.addAll(recipe.getInputs().keySet());
            testResources.addAll(recipe.getOutputs().keySet());
            for (String resourceId : testResources) {
                if (!getResourceRegistry().containsKey(resourceId)) {
                    throw new Error("\"" + resourceId + "\" is not a valid resource in the resource registry.");
                }
            }

            for (String technology : getRegistry(Technology.class).keySet()) {
                if (!getRegistry(Technology.class).containsKey(technology)) {
                    throw new Error("\"" + technology + "\" is not a valid technology in the technology registry.");
                }
            } 
        }
        for (Technology technology : getRegistry(Technology.class).values()) {
            for (String resource : technology.getCost().keySet()) {
                if (!getResourceRegistry().containsKey(resource)) {
                    throw new Error("\"" + technology + "\" is not a valid resource in the resource registry.");
                }
            } 
            for (String technology2 : technology.getPrerequisiteTechnologies()) {
                if (!getRegistry(Technology.class).containsKey(technology2)) {
                    throw new Error("\"" + technology + "\" is not a valid technology in the technology registry.");
                }
            }
        }
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
