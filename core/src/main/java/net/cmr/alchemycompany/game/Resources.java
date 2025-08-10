package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.Map;

public class Resources {

    public enum Resource {
        GOLD(),
        SCIENCE(),
        WATER(),
        COPPER(),
        IRON(),
        SULFUR(),
        SULFURIC_ACID(),
        CRYSTAL(),
        WITCH_EYE(),
        ;

        Resource() {

        }

        @Override
        public String toString() {
            return "RESOURCE."+name();
        }
    }

    public static HashMap<Resource, Float> singleItem(Resource resource, Float amount) {
        HashMap<Resource, Float> map = new HashMap<>();
        map.put(resource, amount);
        return map;
    }

    public static HashMap<Resource, Float> allItems(Float amount) {
        HashMap<Resource, Float> map = new HashMap<>();
        for (Resource resource : Resource.values()) {
            map.put(resource, amount);
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

