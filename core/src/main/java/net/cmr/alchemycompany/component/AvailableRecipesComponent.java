package net.cmr.alchemycompany.component;

import java.util.HashMap;
import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.World;
import net.cmr.alchemycompany.world.World.WorldFeature;

public class AvailableRecipesComponent extends Component {
    
    private HashSet<String> availableRecipes;
    private HashMap<String, WorldFeature[]> tileSpecificRecipes;

    public AvailableRecipesComponent() {}

    public AvailableRecipesComponent(HashSet<String> availableRecipes) {
        this.availableRecipes = availableRecipes;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "availableRecipes");
        json.writeObjectStart("tileSpecificRecipes");
        for (String recipe : tileSpecificRecipes.keySet()) {
            json.writeArrayStart(recipe);
            for (WorldFeature feature : tileSpecificRecipes.get(recipe)) {
                json.writeValue(feature.toString());
            }
            json.writeArrayEnd();
        }
        json.writeObjectEnd();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(Json json, JsonValue jsonData) {
        availableRecipes = json.readValue(
            HashSet.class,
            String.class,
            new HashSet<>(),
            jsonData.get("availableRecipes")
        );
        
        this.tileSpecificRecipes = new HashMap<>();
        JsonValue tileSpecificRecipes = jsonData.get("tileSpecificRecipes");
        if (tileSpecificRecipes != null) {
            for (JsonValue entry = tileSpecificRecipes.child; entry != null; entry = entry.next) {
                String key = entry.name;
                String[] stringArray = entry.asStringArray();
                WorldFeature[] worldFeatures = new WorldFeature[stringArray.length];
                for (int i = 0; i < stringArray.length; i++) {
                    worldFeatures[i] = WorldFeature.valueOf(stringArray[i]);
                }
                this.tileSpecificRecipes.put(key, worldFeatures);
            }
        }
    }

    public HashSet<String> getAvailableRecipes(Entity entity, World world) {
        TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
        if (tpc == null) throw new IllegalArgumentException("Entity must have a tile position component.");
        return getAvailableRecipes(world.getTile(tpc.tileX, tpc.tileY).getFeature());
    }

    public HashSet<String> getAvailableRecipes(WorldFeature aboveFeature) {
        HashSet<String> intermediateRecipeSet = new HashSet<>(availableRecipes);
        if (aboveFeature == null) { return intermediateRecipeSet; }
        for (String recipeID : tileSpecificRecipes.keySet()) {
            WorldFeature[] validFeatures = tileSpecificRecipes.get(recipeID);
            for (WorldFeature feature : validFeatures) {
                if (aboveFeature == feature) {
                    intermediateRecipeSet.add(recipeID);
                    break;
                }
            }
        }
        return intermediateRecipeSet;
    }

}
