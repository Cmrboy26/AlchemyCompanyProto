package net.cmr.alchemycompany;

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
    }

    public static Map<Resource, Float> singleItem(Resource resource, Float amount) {
        Map<Resource, Float> map = new HashMap<>();
        map.put(resource, amount);
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
