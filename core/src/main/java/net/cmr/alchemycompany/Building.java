package net.cmr.alchemycompany;

import java.util.function.BiFunction;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World.WorldFeature;

public abstract class Building {

    private SpriteType type;
    private Player player;
    private int x, y;

    public Building(SpriteType type, BuildingContext context) {
        this.type = type;
        this.player = context.player;
        this.x = context.x;
        this.y = context.y;
    }

    public SpriteType getSpriteType() {
        return type;
    }

    public void renderTile(SpriteBatch batch, float tileSize) {
        batch.draw(Sprites.getTexture(type), x * tileSize, y * tileSize, tileSize, tileSize);
    }

    public Table onClick(Skin skin) {
        Table table = new Table(skin);

        table.add("Owner: " + player.toString()).fillX().pad(10).row();
        table.add("Name: " + getClass().getSimpleName().replaceAll("Building", "")).fillX().pad(10).row();

        return table;
    }

    public WorldFeature[] getAllowedTiles() {
        return WorldFeature.values();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Player getPlayer() {
        return player;
    }

    @FunctionalInterface
    public static interface BuildingConstructorFunction {
        Building apply(BuildingContext context);
    }

    public static class BuildingContext {
        public Player player;
        public int x, y;
        public BuildingContext(Player player, int x, int y) {
            this.player = player;
            this.x = x;
            this.y = y;
        }
    }

    // Buildings

    public static class HeadquarterBuilding extends Building {
        public HeadquarterBuilding(BuildingContext context) { super(SpriteType.HEADQUARTERS, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST, WorldFeature.CRYSTAL_VALLEY }; }
    }

    public static class ExtractorBuilding extends Building {
        public ExtractorBuilding(BuildingContext context) { super(SpriteType.EXTRACTOR, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.WATER, WorldFeature.SWAMP, WorldFeature.MOUNTAINS, WorldFeature.CRYSTAL_VALLEY }; }
    }

    public static class StorageBuilding extends Building {
        public StorageBuilding(BuildingContext context) { super(SpriteType.STORAGE, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP }; }
    }

    public static class FactoryBuilding extends Building {
        public FactoryBuilding(BuildingContext context) { super(SpriteType.FACTORY, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP }; }
    }

    public static class StatueBuilding extends Building {
        public StatueBuilding(BuildingContext context) { super(SpriteType.STATUE, context); }
    }

    public static class ResearchLabBuilding extends Building {
        public ResearchLabBuilding(BuildingContext context) { super(SpriteType.RESEARCH_LAB, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST }; }
    }

    public static class ArcherTowerBuilding extends Building {
        public ArcherTowerBuilding(BuildingContext context) { super(SpriteType.ARCHER_TOWER, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST, WorldFeature.MOUNTAINS, WorldFeature.CRYSTAL_VALLEY }; }
    }

    public static class BarracksBuilding extends Building {
        public BarracksBuilding(BuildingContext context) { super(SpriteType.BARRACKS, context); }
        @Override public WorldFeature[] getAllowedTiles() { return new WorldFeature[]{ WorldFeature.PLAINS, WorldFeature.SWAMP, WorldFeature.FOREST }; }
    }

}
