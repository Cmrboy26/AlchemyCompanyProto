package net.cmr.alchemycompany.component;

import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Recipes.Recipe;

public class AvailableRecipesComponent extends Component {
    
    public HashSet<Recipe> availableRecipes;

    public AvailableRecipesComponent() {}

    public AvailableRecipesComponent(HashSet<Recipe> availableRecipies) {
        this.availableRecipes = availableRecipies;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "availableRecipes");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        availableRecipes = json.readValue(
            HashSet.class,
            Recipe.class,
            jsonData.get("availableRecipes")
        );
    }


}
