package net.cmr.alchemycompany.system;

import java.util.Set;
import java.util.UUID;

import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.component.AvailableRecipesComponent;
import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.SelectedRecipeComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.SelectRecipeActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.ecs.IUpdateSystem;
import net.cmr.alchemycompany.game.Recipe;
import net.cmr.alchemycompany.game.Registry;

public class RecipeSystem extends EntitySystem implements IUpdateSystem {

    static Family recipeChangers;
    static Family recipeActionFamily;

    public RecipeSystem() {
        recipeChangers = Family.all(OwnerComponent.class, AvailableRecipesComponent.class);
        recipeActionFamily = Family.all(PlayerActionComponent.class, SelectRecipeActionComponent.class);
    }

    @Override
    public void update(float delta) {
        Set<Entity> entities = engine.getEntities(recipeActionFamily);
        for (Entity action : entities) {
            PlayerActionComponent pac = action.getComponent(PlayerActionComponent.class);
            SelectRecipeActionComponent srac = action.getComponent(SelectRecipeActionComponent.class);

            UUID playerUUID = pac.playerUUID;
            UUID buildingId = srac.getBuildingId();
            String recipeId = srac.recipeId;
            Entity buildingEntity = engine.getEntity(buildingId);

            System.out.println("RecipeSystem recieved action "+pac+"\n"+srac);
            if (trySelectRecipe(playerUUID, buildingEntity, recipeId, engine.as(ACEngine.class))) {
                GameManager.onBuildingChange(playerUUID, buildingEntity, engine);
            }

            engine.removeEntity(action);
        }
    }

    public static boolean trySelectRecipe(UUID playerUUID, Entity buildingEntity, @Null String recipe, ACEngine engine) {
        if (!recipeChangers.matches(buildingEntity)) {
            return false;
        }

        // Verify owner is the same as the entity owner
        OwnerComponent oc = buildingEntity.getComponent(OwnerComponent.class);
        if (!oc.getUUID().equals(playerUUID)) {
            return false;
        }

        // Verify entity can select recipe
        AvailableRecipesComponent arc = buildingEntity.getComponent(AvailableRecipesComponent.class);
        if (!arc.getAvailableRecipes(buildingEntity, engine.getWorld()).contains(recipe) && recipe != null) {
            return false;
        }

        Recipe recipeObj = null;
        if (recipe != null) {
            recipeObj = Registry.getInstance().getRegistry(Recipe.class).get(recipe);
            // Check if owner has the prerequisite techs
            System.out.println("TODO: add technology check");

            ResearchManagementComponent rmc = getResearchComponent(playerUUID, engine); // Just to verify it exists
            if (rmc == null) {
                return false;
            }
            for (String tech : recipeObj.getRequiredTechnologies()) {
                if (!rmc.hasResearched(tech)) {
                    System.out.println("TECH NOT RESEARCHED: "+tech);
                    Thread.dumpStack();
                    // TODO: add tech check to resource display as well and test if this works
                    return false;
                }
            }

        }

        // Remove existing and add selected recipe component
        buildingEntity.removeComponent(SelectedRecipeComponent.class, engine);
        if (recipe != null) {
            buildingEntity.addComponent(new SelectedRecipeComponent(recipe), engine);
        }

        // Remove consumer and producer components
        buildingEntity.removeComponent(ConsumerComponent.class, engine);
        buildingEntity.removeComponent(ProducerComponent.class, engine);

        // Add new consumer and producer components
        if (recipe != null) {
            buildingEntity.addComponent(new ConsumerComponent(recipeObj.getInputs()), engine);
            buildingEntity.addComponent(new ProducerComponent(recipeObj.getOutputs()), engine);
        }

        // Update clients on new components, recalculate resources
        //engine.changedEntity(buildingEntity);
        engine.changedComponent(buildingEntity, ProducerComponent.class);
        engine.changedComponent(buildingEntity, ConsumerComponent.class);
        engine.changedComponent(buildingEntity, SelectedRecipeComponent.class);
        return true;
    }

    private static Entity getResearchHolderEntity(UUID playerUUID, Engine engine) {
        Set<Entity> researchEntities = engine.getEntities(Family.all(OwnerComponent.class, ResearchManagementComponent.class));
        for (Entity entity : researchEntities) {
            OwnerComponent owner = entity.getComponent(OwnerComponent.class);
            if (owner != null && owner.getUUID() != null && owner.getUUID().equals(playerUUID)) {
                return entity;
            }
        }
        return null;
    }

    private static ResearchManagementComponent getResearchComponent(UUID playerUUID, Engine engine) {
        Entity researchEntity = getResearchHolderEntity(playerUUID, engine);
        if (researchEntity == null) {
            throw new IllegalStateException("No ResearchManagementComponent found for player " + playerUUID);
        }
        ResearchManagementComponent researchComponent = researchEntity.getComponent(ResearchManagementComponent.class);
        if (researchComponent == null) {
            throw new IllegalStateException("No ResearchManagementComponent found for player " + playerUUID);
        }
        return researchComponent;
    }

}
