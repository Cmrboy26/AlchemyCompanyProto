package net.cmr.alchemycompany.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import com.badlogic.gdx.math.Vector2;

import net.cmr.alchemycompany.OpenSimplexNoise;

public class World implements Cloneable {

    public enum WorldFeature {
        WATER,
        FORBIDDEN_WATER,
        PLAINS,
        CRYSTAL_VALLEY,
        FOREST,
        ENCHANTED_FOREST,
        MOUNTAINS,
        DWARVEN_MOUNTAINS,
        SWAMP,
        DRAGONS_DEN,
        ;
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

    public WorldType worldType;
    public int width;
    public int height;
    private Tile[][] tiles;
    private long seed;
    public transient OpenSimplexNoise noise;

    public World(WorldType worldType, long seed) {
        this.worldType = worldType;
        this.width = worldType.width;
        this.height = worldType.height;
        this.tiles = new Tile[width][height];
        this.seed = seed;
        this.noise = new OpenSimplexNoise(seed);
        generateWorld();
    }

    public long getSeed() {
        return seed;
    }

    private void generateWorld() {
        // General Land features
        for (BiFunction<Integer, Integer, Tile> generateFunction : getFeatureGenerators()) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile tile = generateFunction.apply(x, y);
                    if (tile != null) {
                        tiles[x][y] = tile;
                    }
                }
            }
        }
    }

    private List<BiFunction<Integer, Integer, Tile>> getFeatureGenerators() {
        List<BiFunction<Integer, Integer, Tile>> worldFeatureList = new ArrayList<>();
        if (noise == null) {
            noise = new OpenSimplexNoise(seed);
        }
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
            return new Tile(landFeature[landFeatureIndex], x, y);
        });
        worldFeatureList.add((Integer x, Integer y) -> {
            double scale = 0.3f;
            double output = noise.eval(x * scale, y * scale, 5);
            if (output <= -.35) {
                return new Tile(WorldFeature.WATER, x, y);
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
                        return new Tile(specialFeature[i], x, y);
                    }
                    index++;
                }
            }
            return null;
        });

        return worldFeatureList;
    }
    public Tile getTile(int x, int y) {
        return tiles[x][y];
    }
    public void setTileFeature(WorldFeature feature, int x, int y) {
        // debug
        tiles[x][y].feature = feature;
    }
    public static int getSpiralRadius(final int radius) {
        return (int) (Math.pow((radius) * 2 + 1, 2));
    }
    public static Iterator<Vector2> getSpiralIterator(final int ix, final int iy) {
        return getSpiralIterator(ix, iy, 1000);
    }
    public static Iterator<Vector2> getSpiralIterator(final int ix, final int iy, final int maxPoints) {
        return new Iterator<Vector2>() {
            // (di, dj) is a vector - direction in which we move right now
            int di = 1;
            int dj = 0;
            // length of current segment
            int segment_length = 1;

            // current position (i, j) and how much of current segment we passed
            int i = 0;
            int j = 0;
            int k = 0;
            int segment_passed = 0;

            @Override
            public boolean hasNext() {
                return k <= maxPoints;
            }

            @Override
            public Vector2 next() {
                k++;
                if (k == 1) {
                    return new Vector2(ix, iy);
                }

                // make a step, add 'direction' vector (di, dj) to current position (i, j)
                i += di;
                j += dj;
                ++segment_passed;

                if (segment_passed == segment_length) {
                    // done with current segment
                    segment_passed = 0;

                    // 'rotate' directions
                    int buffer = di;
                    di = -dj;
                    dj = buffer;

                    // increase segment length if necessary
                    if (dj == 0) {
                        segment_length++;
                    }
                }

                return new Vector2(i + ix, j + iy);
            }

        };
    }

    @Override
    public World clone() {
        try {
            World cloned = (World) super.clone();
            cloned.tiles = new Tile[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    cloned.tiles[x][y] = tiles[x][y] != null ? tiles[x][y].clone() : null;
                }
            }
            cloned.noise = new OpenSimplexNoise(seed);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}
