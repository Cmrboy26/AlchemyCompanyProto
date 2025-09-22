package net.cmr.alchemycompany.system;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.ResearchRequirementComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.ResearchActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.ecs.IUpdateSystem;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Technology;

/**
 * The {@code ResearchSystem} class manages the research process for players in the game.
 * It handles starting, stopping, and progressing research on technologies, as well as
 * checking prerequisites and consuming resources required for research.
 * <p>
 * This system interacts with entities containing {@link ResearchManagementComponent},
 * {@link PlayerActionComponent}, and {@link ResearchActionComponent}, and coordinates
 * research actions based on player input and game state.
 * </p>
 *
 * <ul>
 *   <li>Processes research actions for entities.</li>
 *   <li>Checks if technologies can be researched based on prerequisites.</li>
 *   <li>Estimates the number of turns required to complete research.</li>
 *   <li>Consumes resources for ongoing research and marks technologies as researched when complete.</li>
 *   <li>Provides utility methods for querying research status and requirements.</li>
 * </ul>
 */
public class ResearchSystem extends EntitySystem implements IUpdateSystem {

    @Override
    public void update(float delta) {
        IActionComponent.processActionEntities(engine, ResearchActionComponent.class, (entity) -> {
            PlayerActionComponent pac = entity.getComponent(PlayerActionComponent.class);
            ResearchActionComponent rac = entity.getComponent(ResearchActionComponent.class);
            Entity rmcEntity = getPlayerResearchManagerEntity(pac.playerUUID);
            ResearchManagementComponent rmc = rmcEntity.getComponent(ResearchManagementComponent.class);
            Technology tech = Registry.getInstance().getRegistry(Technology.class).get(rac.getResearchId());
            if (tech == null) {
                return; // Invalid technology
            }
            if (rmc.getCurrentResearch() != null && rmc.getCurrentResearch().equals(tech)) {
                // Already researching this technology, ignore
                if (!rac.isStarts()) {
                    rmc.stopResearch(); 
                    engine.changedComponent(entity, ResearchManagementComponent.class);
                    System.out.println("STOPPEDs RESEARCH "+rmc.isResearching());
                }
                return;
            }
            if (rmc.hasResearched(tech.getId())) {
                // Already researched this technology, ignore
                return;
            }
            if (!rmc.prerequisitesMet(tech.getId())) {
                // Prereqs not met
                return;
            }
            rmc.stopResearch();
            if (rac.isStarts()) {
                rmc.startResearch(tech.getId());
            }
            engine.changedComponent(entity, ResearchManagementComponent.class);
            System.out.println("QUEUED RESEARCH "+rmc.isResearching());
        });
    }

    /**
     * Retrieves the {@link ResearchManagementComponent} associated with the specified player.
     *
     * @param playerUUID the unique identifier of the player
     * @return the {@link ResearchManagementComponent} for the given player, or {@code null} if not found
     */
    public ResearchManagementComponent getPlayerResearchManager(UUID playerUUID) {
        return getPlayerResearchManagerEntity(playerUUID).getComponent(ResearchManagementComponent.class);
    }

    /**
     * Retrieves the {@link Entity} that manages research for the specified player.
     * <p>
     * Searches through all entities that have both {@link ResearchManagementComponent}
     * and {@link OwnerComponent}, and returns the entity whose {@code playerUUID}
     * matches the provided {@code playerUUID}.
     * </p>
     *
     * @param playerUUID the unique identifier of the player whose research manager entity is to be retrieved
     * @return the {@link Entity} associated with the given player ID
     * @throws IllegalStateException if no entity with a {@link ResearchManagementComponent} and matching {@code playerUUID} is found
     */
    public Entity getPlayerResearchManagerEntity(UUID playerUUID) {
        for (Entity entity : engine.getEntities(Family.all(ResearchManagementComponent.class, OwnerComponent.class))) {
            ResearchManagementComponent rmc = entity.getComponent(ResearchManagementComponent.class);
            OwnerComponent oc = entity.getComponent(OwnerComponent.class);
            if (oc.getUUID().equals(playerUUID)) {
                return entity;
            }
        }
        throw new IllegalStateException("No ResearchManagementComponent found for player " + playerUUID);
    }

    
    /**
     * Checks if the specified player has researched the given technology.
     *
     * @param playerUUID the unique identifier of the player
     * @param technologyId the unique identifier of the technology
     * @return {@code true} if the player has researched the technology; {@code false} otherwise
     */
    public boolean hasTechnology(UUID playerUUID, String technologyId) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerUUID);
        return rmc.hasResearched(technologyId);
    }

    /**
     * Determines whether a player can research a specific technology.
     *
     * @param playerUUID      the unique identifier of the player
     * @param technologyId  the unique identifier of the technology to check
     * @return              true if the player meets all prerequisites to research the technology; false otherwise
     */
    public boolean canResearchTechnology(UUID playerUUID, String technologyId) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerUUID);
        return rmc.prerequisitesMet(technologyId);
    }

    /**
     * Estimates the number of turns required for a player to research a specific technology.
     *
     * @param playerUUID the unique identifier of the player
     * @param technologyId the identifier of the technology to be researched
     * @return the estimated number of turns needed to complete the research
     */
    public int estimateTurnCount(UUID playerUUID, String technologyId) {
        Technology technology = Registry.getInstance().getRegistry(Technology.class).get(technologyId);
        return estimateTurnCount(playerUUID, technology.getCost());
    }

    /**
     * Estimates the number of turns required for a player to complete their current research.
     *
     * @param playerUUID the unique identifier of the player whose research progress is being estimated
     * @return the estimated number of turns remaining to complete the research
     */
    public int estimateTurnCount(UUID playerUUID) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerUUID);
        return estimateTurnCount(playerUUID, rmc.getCostRemaining());
    }

    /**
     * Estimates the number of turns required for a player to fulfill the remaining research costs.
     * <p>
     * This method calculates the maximum number of turns needed based on the player's current
     * expendable resources, which are determined by summing both the resources generated per second
     * and the stored resources. For each required resource, it computes how many turns are needed
     * to cover the remaining cost, and returns the highest value among all resources.
     * If any required resource is unavailable, {@code Integer.MAX_VALUE} is returned.
     *
     * @param playerUUID       the unique identifier of the player
     * @param costsRemaining   a map of resource names to the remaining amount required for research
     * @return the estimated number of turns needed to fulfill all remaining costs, or {@code Integer.MAX_VALUE}
     *         if any required resource is unavailable
     */
    public int estimateTurnCount(UUID playerUUID, Map<String, Float> costsRemaining) {
        ResourceSystem rs = engine.getSystem(ResourceSystem.class);
        Map<String, Float> expendableResources = new HashMap<>();
        rs.getCachedResourcePerSecond(playerUUID).entrySet().stream().forEach((e) -> {
            
            expendableResources.put(e.getKey(), expendableResources.getOrDefault(e.getKey(), 0f) + e.getValue());
        });
        rs.getCachedStoredResources(playerUUID).entrySet().stream().forEach((e) -> {
            expendableResources.put(e.getKey(), expendableResources.getOrDefault(e.getKey(), 0f) + e.getValue());
        });
        int maxTurns = -1;
        for (Entry<String, Float> entry : costsRemaining.entrySet()) {
            Float expendableAmount = expendableResources.getOrDefault(entry.getKey(), 0f);
            if (expendableAmount == 0) {
                return Integer.MAX_VALUE;
            }
            int turnCalculation = (int) Math.ceil(entry.getValue() / expendableAmount);
            if (turnCalculation > maxTurns) {
                maxTurns = turnCalculation;
            }
        }
        return maxTurns;
    }

    public void consumeAvailableResources(UUID playerUUID, final Map<String, Float> storedResources) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerUUID);
        if (rmc.isResearching()) {
            HashMap<String, Float> costRemaining = rmc.getCostRemaining();
            boolean allCostsPaid = true;
            for (String resourceID : costRemaining.keySet()) {
                float storageRemoveAmount = Math.min(costRemaining.get(resourceID), storedResources.get(resourceID));
                assert storageRemoveAmount >= 0;
                storedResources.put(resourceID, storedResources.get(resourceID) - storageRemoveAmount);
                costRemaining.put(resourceID, costRemaining.get(resourceID) - storageRemoveAmount);
                allCostsPaid = costRemaining.get(resourceID) == 0;
                assert costRemaining.get(resourceID) >= 0;
            }
            rmc.setCostRemaining(costRemaining);
            if (allCostsPaid) {
                rmc.setResearched(rmc.getCurrentResearchID());
                rmc.stopResearch();
            }
            this.engine.changedComponent(getPlayerResearchManagerEntity(playerUUID), ResearchManagementComponent.class);
        }
    }

    public boolean hasMetRequirements(UUID playerUUID, ResearchRequirementComponent rrc) {
        // Check if the player has met all research requirements
        ResearchManagementComponent rmc = getPlayerResearchManager(playerUUID);
        if (rmc == null) {
            return false;
        }
        for (String req : rrc.technologiesRequired) {
            if (!rmc.hasResearched(req)) {
                return false;
            }
        }
        return true;
    }
    
}
