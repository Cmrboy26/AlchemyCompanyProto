package net.cmr.alchemycompany.component;

import java.util.Set;

import net.cmr.alchemycompany.ecs.Component;
import net.cmr.alchemycompany.game.Recipes.Recipe;

public class AvailableRecipiesComponent extends Component {
    
    public Set<Recipe> availableRecipies;

    public AvailableRecipiesComponent(Set<Recipe> availableRecipies) {
        this.availableRecipies = availableRecipies;
    }


}
