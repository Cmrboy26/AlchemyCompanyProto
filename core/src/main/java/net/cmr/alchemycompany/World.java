package net.cmr.alchemycompany;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import com.badlogic.gdx.math.Vector2;

import net.cmr.alchemycompany.Building.BuildingContext;
import net.cmr.alchemycompany.Building.HeadquarterBuilding;
import net.cmr.alchemycompany.troops.Troop;

public class World {

    public enum WorldFeature {
        WATER,
        PLAINS,
        FOREST,
        MOUNTAINS,
        SWAMP,
        CRYSTAL_VALLEY,
    }

    public enum WorldType {
        SMALL(30, 30, 1),
        MEDIUM(60, 60, 2),
        LARGE(90, 90, 3);

        private final int width;
        private final int height;
        private final int specialFeatureRepeat;
        WorldType(int width, int height, int specialFeatureRepeat) {
            this.width = width;
            this.height = height;
            this.specialFeatureRepeat = specialFeatureRepeat;
        }
    }

    public final WorldType worldType;
    public final int width;
    public final int height;
    private final WorldFeature[][] features;
    private final Building[][] buildings;
    private final List<Troop> troops;
    private final long seed;

    public World(WorldType worldType, long seed) {
        this.worldType = worldType;
        this.width = worldType.width;
        this.height = worldType.height;
        this.features = new WorldFeature[width][height];
        this.buildings = new Building[width][height];
        this.troops = new ArrayList<>();
        this.seed = seed;
        generateWorld();
    }

    private void generateWorld() {
        // General Land features
        for (BiFunction<Integer, Integer, WorldFeature> generateFunction : getFeatureGenerators()) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    WorldFeature feature = generateFunction.apply(x, y);
                    if (feature != null) {
                        features[x][y] = feature;
                    }
                    buildings[x][y] = null; // No buildings initially
                }
            }
        }
    }

    private List<BiFunction<Integer, Integer, WorldFeature>> getFeatureGenerators() {
        List<BiFunction<Integer, Integer, WorldFeature>> worldFeatureList = new ArrayList<>();
        final OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        worldFeatureList.add((Integer x, Integer y) -> {
            WorldFeature[] landFeature = new WorldFeature[] {
                WorldFeature.PLAINS,
                WorldFeature.PLAINS,
                WorldFeature.FOREST,
                WorldFeature.MOUNTAINS
            };
            double scale = 0.5f;
            int landFeatureIndex = (int) ((noise.eval(x * scale, y * scale, 0) + 1) / 2 * landFeature.length);
            landFeatureIndex = Math.max(0, Math.min(landFeatureIndex, landFeature.length - 1));
            return landFeature[landFeatureIndex];
        });
        worldFeatureList.add((Integer x, Integer y) -> {
            double scale = 0.3f;
            double output = noise.eval(x * scale, y * scale, 5);
            if (output <= -.35) {
                return WorldFeature.WATER;
            }
            return null;
        });
        worldFeatureList.add((Integer x, Integer y) -> {
            WorldFeature[] specialFeature = new WorldFeature[] {
                WorldFeature.SWAMP,
                WorldFeature.CRYSTAL_VALLEY,
                WorldFeature.WATER
            };
            int[] repeats = new int[] {
                (int) Math.pow(worldType.specialFeatureRepeat + 1, 2),
                (int) Math.pow(worldType.specialFeatureRepeat + 1, 2),
                (int) Math.pow(worldType.specialFeatureRepeat + 1, 2)
            };

            int featureSize = 0;
            for (int i = 0; i < specialFeature.length; i++) {
                featureSize += repeats[i];
            }
            Random random = new Random(seed);
            Vector2[] specialPositions = new Vector2[featureSize];
            for (int i = 0; i < featureSize; i++) {
                specialPositions[i] = new Vector2(random.nextFloat() * width, random.nextFloat() * height);
            }
            double[] size = new double[featureSize];
            for (int i = 0; i < featureSize; i++) {
                size[i] = random.nextFloat() * 1f + 1;
            }
            int index = 0;
            for (int i = 0; i < repeats.length; i++) {
                for (int j = 0; j < repeats[i]; j++) {
                    Vector2 iteratePosition = specialPositions[index];
                    double iterateSize = size[index];
                    double distance = Math.sqrt(Math.pow(x - iteratePosition.x, 2) + Math.pow(y - iteratePosition.y, 2));
                    if (distance <= iterateSize) {
                        //System.out.println(specialFeature[i]);
                        return specialFeature[i];
                    }
                    index++;
                }
            }
            return null;
        });

        return worldFeatureList;
    }

    public void addBuilding(Building building) {
        buildings[building.getX()][building.getY()] = building;
        building.getPlayer().addBuilding(building);
    }

    public void removeBuilding(Building building) {
        buildings[building.getX()][building.getY()] = null;
        building.getPlayer().removeBuilding(building);
    }

    public WorldFeature getFeature(int x, int y) {
        return features[x][y];
    }

    public Building getBuilding(int x, int y) {
        return buildings[x][y];
    }

    public Vector2 placeHeadquarters(Player player) {
        Random random = new Random(seed * seed);
        while (true) {
            int x = (int) (random.nextDouble() * width);
            int y = (int) (random.nextDouble() * height);
            System.out.println(x + ", " + y);
            HeadquarterBuilding hq = new HeadquarterBuilding(new BuildingContext(this, player, x, y));
            for (WorldFeature feature : hq.getAllowedTiles()) {
                if (feature == features[x][y]) {
                    addBuilding(hq);
                    return new Vector2(x, y);
                }
            }
        }
    }

    public List<Troop> getTroops() {
        return troops;
    }

    public boolean placeTroop(Troop troop) {
        for (Troop i : troops) {
            if (troop.getX() == i.getX() && troop.getY() == i.getY()) {
                return false;
            }
        }
        troops.add(troop);
        return true;
    }

    public Troop getTroopAt(int x, int y) {
        for (Troop i : troops) {
            if (i.getX() == x && i.getY() == y) {
                return i;
            }
        }
        return null;
    }

}
