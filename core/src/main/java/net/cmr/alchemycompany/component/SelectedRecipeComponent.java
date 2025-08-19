package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SelectedRecipeComponent extends Component {
    
    public String selectedRecipe;

    public SelectedRecipeComponent() { }
    public SelectedRecipeComponent(String recipeId) {
        this.selectedRecipe = recipeId;
    }

    @Override
    public void write(Json json) {
        json.writeValue("selectedRecipe", selectedRecipe);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        selectedRecipe = jsonData.getString("selectedRecipe", null);
    }

}
