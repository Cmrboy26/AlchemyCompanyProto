package net.cmr.alchemycompany;

public class World {
    
    public enum WorldFeature {
        PLAINS,
        FOREST,
        MOUNTAINS,
        SWAMP,
        CRYSTAL_VALLEY,
    }

    public enum Building {
        HEADQUARTERS,
        EXTRACTOR,
        STORAGE,
        FACTORY,
        STATUE,
        RESEARCH_LAB,
        ARCHER_TOWER,
        BARRACKS
    }

    public enum WorldType {
        SMALL(30, 30),
        MEDIUM(60, 60),
        LARGE(90, 90);

        private final int width;
        private final int height;
        WorldType(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public final WorldType worldType;
    public final int width;
    public final int height;
    public final WorldFeature[][] features;
    public final Building[][] buildings;

    public World(WorldType worldType) {
        this.worldType = worldType;
        this.width = worldType.width;
        this.height = worldType.height;
        this.features = new WorldFeature[width][height];
        this.buildings = new Building[width][height];
        generateWorld();
    }

    private void generateWorld() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                features[x][y] = generateRandomFeature(x, y);
                buildings[x][y] = null; // No buildings initially
            }
        }
    }

    private WorldFeature generateRandomFeature(int x, int y) {
        OpenSimplexNoise noise = new OpenSimplexNoise(System.currentTimeMillis());
        WorldFeature[] features = new WorldFeature[] {
            WorldFeature.SWAMP,
            WorldFeature.PLAINS,
            WorldFeature.PLAINS,
            WorldFeature.FOREST,
            WorldFeature.MOUNTAINS,
            WorldFeature.CRYSTAL_VALLEY
        };

        int randomIndex = (int) ((noise.eval(x * 0.5, y * 0.5, 0) + 1) / 2 * features.length);
        randomIndex = Math.max(0, Math.min(randomIndex, features.length - 1));
        return features[randomIndex];
    }

}
