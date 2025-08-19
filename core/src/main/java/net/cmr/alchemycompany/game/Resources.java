package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.Map;

public class Resources {

    public static HashMap<String, Float> singleItem(String resourceId, Float amount) {
        HashMap<String, Float> map = new HashMap<>();
        map.put(resourceId, amount);
        return map;
    }

    public static HashMap<String, Float> allItems(Float amount, boolean includePerTurn) {
        HashMap<String, Float> map = new HashMap<>();
        for (Resource resource : Registry.getInstance().getRegistry(Resource.class).values()) {
            if (includePerTurn || !resource.isPerTurnResource()) {
                map.put(resource.getId(), amount);
            }
        }
        return map;
    }

    private Map<Resource, Float> resources;
    public Resources() {
        resources = new HashMap<>();
    }

    public static Resources start() {
        return new Resources();
    }
    public Resources add(Resource r, Float amount) {
        resources.put(r, resources.getOrDefault(r, 0f) + amount);
        return this;
    }
    public Map<Resource, Float> build() {
        return resources;
    }

}

