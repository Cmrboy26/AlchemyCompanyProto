package net.cmr.alchemycompany.component;

import java.util.HashSet;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.game.Recipe;

public class AvailableRecipesComponent extends Component {
    
    public HashSet<String> availableRecipes;

    public AvailableRecipesComponent() {}

    public AvailableRecipesComponent(HashSet<String> availableRecipes) {
        this.availableRecipes = availableRecipes;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "availableRecipes");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(Json json, JsonValue jsonData) {
        availableRecipes = json.readValue(
            HashSet.class,
            String.class,
            jsonData.get("availableRecipes")
        );
    }


}
