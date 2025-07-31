package net.cmr.alchemycompany.component;

import java.util.HashSet;
import java.util.Set;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.entity.BuildingFactory.BuildingType;
import net.cmr.alchemycompany.world.World.WorldFeature;

public class BuildingComponent extends Component {

    public BuildingType buildingType;
    public Set<WorldFeature> validPlacement;

    public BuildingComponent(BuildingType buildingType, Set<WorldFeature> validPlacement) {
        this.buildingType = buildingType;
        this.validPlacement = validPlacement;
    }

    public static Set<WorldFeature> only(WorldFeature...features) {
        Set<WorldFeature> set = new HashSet<>();
        for (WorldFeature feature : features) {
            set.add(feature);
        }
        return set;
    }

    public static Set<WorldFeature> exclude(WorldFeature...features) {
        Set<WorldFeature> set = new HashSet<>();
        for (WorldFeature feature : WorldFeature.values()) {
            boolean excluded = false;
            for (WorldFeature checkFeature : features) {
                if (feature == checkFeature) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                set.add(feature);
            }
        }
        return set;
    }

}
