package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.cmr.alchemycompany.Building.ArcherTowerBuilding;
import net.cmr.alchemycompany.Building.BarracksBuilding;
import net.cmr.alchemycompany.Building.BuildingConstructorFunction;
import net.cmr.alchemycompany.Building.BuildingContext;
import net.cmr.alchemycompany.Building.ExtractorBuilding;
import net.cmr.alchemycompany.Building.FactoryBuilding;
import net.cmr.alchemycompany.Building.ResearchLabBuilding;
import net.cmr.alchemycompany.Building.StatueBuilding;
import net.cmr.alchemycompany.Building.StorageBuilding;
import net.cmr.alchemycompany.ResearchManager.Technology;
import net.cmr.alchemycompany.Resources.Resource;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World.WorldFeature;

public class Shop {

    // Provide context for the building being created (just in case certain biomes
    // should make it cheaper)
    // public Building createBuilding(ShopOption option, Player player, World world,
    // int x, int y) {}

    public enum ShopCategory {
        BUILDINGS,
    }

    public static boolean EVERYTHING_FREE = false;

    public static class Cost {

        public enum CostType {
            RESOURCE,
            SCIENCE,
        }

        public final CostType type;
        public final Map<Resource, Float> resources;
        public final Technology requiredTechnology;

        public Cost(CostType type, Map<Resource, Float> resources, Technology requiredTechnology) {
            this.type = type;
            this.resources = resources;
            this.requiredTechnology = requiredTechnology;
        }

        public static Cost res(Map<Resource, Float> resources) {
            return new Cost(CostType.RESOURCE, resources, null);
        }

    }

    public static class ResourceCostConstructor {
        private final Map<Resource, Float> resources;
        private Technology technology;

        public ResourceCostConstructor() {
            resources = new HashMap<>();
            technology = null;
        }

        public static ResourceCostConstructor create() {
            return new ResourceCostConstructor();
        }

        public ResourceCostConstructor add(Resource resource, Number amount) {
            resources.put(resource, amount.floatValue());
            return this;
        }

        public ResourceCostConstructor tech(Technology tech) {
            this.technology = tech;
            return this;
        }

        public Cost build() {
            resources.entrySet().removeIf(entry -> entry.getValue() == 0f);
            return new Cost(Cost.CostType.RESOURCE, resources, technology);
        }
    }

    public enum ShopOption {
        EXTRACTOR(ShopCategory.BUILDINGS, SpriteType.EXTRACTOR, 2, ExtractorBuilding::new,
                "Extractor", "Extracts resources from the ground",
                context -> {
                    WorldFeature over = context.world.getFeature(context.x, context.y);
                    int copperAdd = 0;
                    if (over != null && over == WorldFeature.WATER) {
                        copperAdd = 4;
                    } else if (over != null && over == WorldFeature.CRYSTAL_VALLEY) {
                        copperAdd = 6;
                    }

                    return ResourceCostConstructor.create()
                        .add(Resource.GOLD, 1 + context.player.getCountOf(ExtractorBuilding.class) * 2)
                        .add(Resource.COPPER, copperAdd)
                        .tech(Technology.MINING)
                        .build();
                }),
        STORAGE(ShopCategory.BUILDINGS, SpriteType.STORAGE, 2, StorageBuilding::new,
                "Storage", "Stores resources for later use",
                context -> ResourceCostConstructor.create().add(Resource.IRON, context.player.getCountOf(StorageBuilding.class) * 2).tech(Technology.STORAGE).build()),
        FACTORY(ShopCategory.BUILDINGS, SpriteType.FACTORY, 3, FactoryBuilding::new,
                "Factory", "Processes raw materials into finished goods",
                context -> ResourceCostConstructor.create().add(Resource.COPPER, context.player.getCountOf(FactoryBuilding.class) * 2 + 1).add(Resource.IRON, context.player.getCountOf(FactoryBuilding.class) + 1).tech(Technology.FACTORY).build()),
        STATUE(ShopCategory.BUILDINGS, SpriteType.STATUE, 2, StatueBuilding::new,
                "Statue", "An inspirational monument that inspires scientific progress",
                context -> ResourceCostConstructor.create().add(Resource.IRON, 4).build()),
        RESEARCH_LAB(ShopCategory.BUILDINGS, SpriteType.RESEARCH_LAB, 4, ResearchLabBuilding::new,
                "Research Lab", "A place where scientific research is conducted to unlock new technologies",
                context -> ResourceCostConstructor.create().add(Resource.IRON, Math.pow(context.player.getCountOf(ResearchLabBuilding.class) + 1, 2)).add(Resource.CRYSTAL, context.player.getCountOf(ResearchLabBuilding.class) + 1).tech(Technology.RESEARCH_LAB).build()),
        ARCHER_TOWER(ShopCategory.BUILDINGS, SpriteType.ARCHER_TOWER, 2, ArcherTowerBuilding::new,
                "Archer Tower", "A defensive structure that protects your base from enemies",
                context -> ResourceCostConstructor.create().add(Resource.WITCH_EYE, 3).add(Resource.COPPER, 2).tech(Technology.MILITARY).build()),
        BARRACKS(ShopCategory.BUILDINGS, SpriteType.BARRACKS, 3, BarracksBuilding::new,
                "Barracks", "A military building that trains soldiers for defense and attack",
                context -> ResourceCostConstructor.create().add(Resource.IRON, Math.pow(context.player.getCountOf(BarracksBuilding.class) + 1, 2)).tech(Technology.MILITARY).build()),
                ;

        public final ShopCategory category;
        public final SpriteType icon;
        public final int turns;
        public final BuildingConstructorFunction constructor;
        public final String name, description;
        public final Function<BuildingContext, Cost> costFunction;

        ShopOption(ShopCategory category, SpriteType icon, int turns, BuildingConstructorFunction constructor,
                String name, String description,
                Function<BuildingContext, Cost> costFunction) {
            {
                this.category = category;
                this.icon = icon;
                this.turns = turns;
                this.constructor = constructor;
                this.name = name;
                this.description = description;
                this.costFunction = costFunction;
            }
        }

    }
}