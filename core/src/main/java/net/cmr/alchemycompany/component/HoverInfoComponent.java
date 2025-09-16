package net.cmr.alchemycompany.component;

import java.util.UUID;
import java.util.function.Consumer;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.game.Recipe;
import net.cmr.alchemycompany.game.Registry;

public class HoverInfoComponent extends Component {
    
    public String hoverText;

    public HoverInfoComponent() {}

    public HoverInfoComponent(String hoverText) {
        this.hoverText = hoverText;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "hoverText");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.hoverText = json.readValue("hoverText", String.class, jsonData);
    }

    private static <T extends Component> void processComponent(Component component, Class<T> targetComponentClass, Consumer<T> callable) {
        if (targetComponentClass.isInstance(component)) {
            callable.accept((T) component);
        }
    }

    public static String getAutomaticHoverString(UUID playerUUID, Entity entity) {
        StringBuilder builder = new StringBuilder();
        
        for (Component component : entity.getComponents().values()) {
            final StringBuilder intermediateBuilder = new StringBuilder();

            // Perform actions

            processComponent(component, HealthComponent.class, (hc) -> {
                intermediateBuilder.append("$image@hp&width=24&height=24& $");
                intermediateBuilder.append(HealthComponent.class.getName());
                intermediateBuilder.append("@health&round=1  / $");
                intermediateBuilder.append(HealthComponent.class.getName());
                intermediateBuilder.append("@maxHealth&round=1  HP");
            });
            processComponent(component, AttackComponent.class, (hc) -> {
                intermediateBuilder.append("$image@strength&width=24&height=24& $");
                intermediateBuilder.append(AttackComponent.class.getName());
                intermediateBuilder.append("@baseAttack&round=1  Attack");
            });
            processComponent(component, MovementComponent.class, (hc) -> {
                intermediateBuilder.append("$image@movement&width=24&height=24& $");
                intermediateBuilder.append(MovementComponent.class.getName());
                intermediateBuilder.append("@movesRemaining&round=1  / $");
                intermediateBuilder.append(MovementComponent.class.getName());
                intermediateBuilder.append("@movesPerTurn&round=1  Moves");
            });
            processComponent(component, SelectedRecipeComponent.class, (src) -> {
                Recipe recipe = Registry.getInstance().getRegistry(Recipe.class).get(src.selectedRecipe);
                
                intermediateBuilder.append("$image@" + recipe.getIcon() + "&width=24&height=24& ");
                intermediateBuilder.append(recipe.getName());
            });
            processComponent(component, OwnerComponent.class, (oc) -> {
                if (!oc.playerID.equals(playerUUID.toString())) {
                    //intermediateBuilder.append(playerUUID.toString());
                }
            });
            processComponent(component, ConstructionComponent.class, (oc) -> {
                intermediateBuilder.append("$image@construction&width=24&height=24& $");
                intermediateBuilder.append(ConstructionComponent.class.getName());
                intermediateBuilder.append("@turns  Turns Left");
            });

            if (intermediateBuilder.length() > 0) {
                builder.append(intermediateBuilder.toString());
                builder.append(" $\n ");
            }
        }
        return builder.toString();
    }

}
