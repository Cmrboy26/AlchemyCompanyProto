package net.cmr.alchemycompany.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Registry that loads and manages all available resources from JSON data.
 * This provides a centralized way to access resources while maintaining backward compatibility.
 */
public class ResourceRegistryo {
    
    private static ResourceRegistryo instance;
    private Map<String, Resource> resourcesById;
    private List<Resource> allResources;
    
    private ResourceRegistryo() {
        resourcesById = new HashMap<>();
        allResources = new ArrayList<>();
        loadResources();
    }
    
    public static ResourceRegistryo getInstance() {
        if (instance == null) {
            instance = new ResourceRegistryo();
        }
        return instance;
    }
    
    private void loadResources() {
        try {
            FileHandle file = Gdx.files.internal("gamedata/resources.json");
            Json json = new Json();
            JsonValue root = json.fromJson(null, file);
            JsonValue resourcesArray = root.get("registry");
            
            for (JsonValue resourceData = resourcesArray.child; resourceData != null; resourceData = resourceData.next) {
                Resource resource = new Resource();
                resource.read(json, resourceData);
                registerResource(resource);
            }
            
            System.out.println("Loaded " + allResources.size() + " resources from resources.json");   
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void registerResource(Resource resource) {
        resourcesById.put(resource.getId(), resource);
        allResources.add(resource);
    }
    
    public Resource getResource(String id) {
        Resource resource = resourcesById.get(id);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + id);
        }
        return resource;
    }
    
    public List<Resource> getAllResources() {
        return new ArrayList<>(allResources);
    }
    public List<Resource> getAllPerTurnResource() {
        List<Resource> perTurnResources = new ArrayList<>();
        for (Resource resource : allResources) {
            if (resource.isPerTurnResource()) {
                perTurnResources.add(resource);
            }
        }
        return perTurnResources;
    }
    public List<Resource> getAllResourcesOmmitPerTurn() {
        List<Resource> nonPerTurnResources = new ArrayList<>();
        for (Resource resource : allResources) {
            if (!resource.isPerTurnResource()) {
                nonPerTurnResources.add(resource);
            }
        }
        return nonPerTurnResources;
    }
    
    public boolean hasResource(String id) {
        return resourcesById.containsKey(id);
    }
}
