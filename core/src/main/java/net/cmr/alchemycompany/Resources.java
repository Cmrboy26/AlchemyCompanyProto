package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.Map;

public class Resources {

    public enum Resource {
        GOLD(),
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

}
