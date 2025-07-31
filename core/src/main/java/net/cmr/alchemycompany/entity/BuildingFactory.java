package net.cmr.alchemycompany.entity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.component.BuildingComponent;
import net.cmr.alchemycompany.component.ConstructionComponent;
import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.DefenseComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.RenderComponent;
import net.cmr.alchemycompany.component.ResearchRequirementComponent;
import net.cmr.alchemycompany.component.StorageComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.game.Effects.AttackType;
import net.cmr.alchemycompany.game.Resources;
import net.cmr.alchemycompany.game.Resources.Resource;
import net.cmr.alchemycompany.game.Technologies.Technology;
import net.cmr.alchemycompany.world.World.WorldFeature;

public class BuildingFactory {
    
    public enum BuildingType {
        HEADQUARTERS,
        FARM
    }

    public static Entity createBuilding(UUID playerID, BuildingType type, int x, int y) {
        Entity building = new Entity();
        building.addComponent(new TilePositionComponent(x, y), null);
        building.addComponent(new OwnerComponent(playerID), null);
        building.addComponent(new HealthComponent(100), null);
        building.addComponent(new ConstructionComponent(5), null);

        SpriteType sprite = null;
        Set<WorldFeature> placeableFeatures = null;
        switch (type) {
            case HEADQUARTERS:
                sprite = SpriteType.HEADQUARTERS;
                placeableFeatures = BuildingComponent.exclude(WorldFeature.WATER);

                //building.addComponent(new ProducerComponent(Resources.singleItem(Resource.IRON, 1f)), null);
                building.addComponent(new StorageComponent(Collections.emptyMap(), Resources.allItems(10f)), null);
                building.addComponent(new DefenseComponent(50, Collections.singletonMap(AttackType.NORMAL, 2f)), null);
                break;
            case FARM:
                sprite = SpriteType.FARM;
                placeableFeatures = BuildingComponent.only(WorldFeature.PLAINS);
                building.addComponent(new ProducerComponent(Resources.singleItem(Resource.IRON, 1f)), null);
                building.addComponent(new ResearchRequirementComponent(Technology.AGRICULTURE), null);
                break;
            default:
                break;
        }
        building.addComponent(new RenderComponent(sprite), null);
        building.addComponent(new BuildingComponent(type, placeableFeatures), null);
        return building;
    }

}
