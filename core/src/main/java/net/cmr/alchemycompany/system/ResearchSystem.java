package net.cmr.alchemycompany.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.PlayerActionComponent;
import net.cmr.alchemycompany.component.actions.ResearchActionComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Technology;

public class ResearchSystem extends EntitySystem implements IUpdateSystem {

    @Override
    public void update(float delta) {
        IActionComponent.processActionEntities(engine, ResearchActionComponent.class, (entity) -> {
            PlayerActionComponent pac = entity.getComponent(PlayerActionComponent.class);
            ResearchActionComponent rac = entity.getComponent(ResearchActionComponent.class);
            Entity rmcEntity = getPlayerResearchManagerEntity(pac.playerUUID.toString());
            ResearchManagementComponent rmc = rmcEntity.getComponent(ResearchManagementComponent.class);
            Technology tech = Registry.getInstance().getRegistry(Technology.class).get(rac.getResearchId());
            if (tech == null) {
                return; // Invalid technology
            }
            if (rmc.getCurrentResearch() != null && rmc.getCurrentResearch().equals(tech)) {
                // Already researching this technology, ignore
                if (!rac.isStarts()) {
                    rmc.stopResearch();
                    engine.changedEntity(rmcEntity);
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
            engine.changedEntity(rmcEntity);
            System.out.println("QUEUED RESEARCH "+rmc.isResearching());
        });
    }

    public ResearchManagementComponent getPlayerResearchManager(String playerID) {
        return getPlayerResearchManagerEntity(playerID).getComponent(ResearchManagementComponent.class);
    }

    public Entity getPlayerResearchManagerEntity(String playerID) {
        for (Entity entity : engine.getEntities(Family.all(ResearchManagementComponent.class, OwnerComponent.class))) {
            ResearchManagementComponent rmc = entity.getComponent(ResearchManagementComponent.class);
            OwnerComponent oc = entity.getComponent(OwnerComponent.class);
            if (oc.playerID.equals(playerID)) {
                return entity;
            }
        }
        throw new IllegalStateException("No ResearchManagementComponent found for player " + playerID);
    }

    public boolean hasTechnology(String playerId, String technologyId) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerId);
        return rmc.hasResearched(technologyId);
    }

    public void consumeAvailableResources(UUID playerUUID, final Map<String, Float> storedResources) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerUUID.toString());
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
                System.out.println(resourceID+ " REMAINING " + costRemaining.get(resourceID));
            }
            rmc.setCostRemaining(costRemaining);
            if (allCostsPaid) {
                rmc.setResearched(rmc.getCurrentResearchID());
                rmc.stopResearch();
            }
            this.engine.changedEntity(getPlayerResearchManagerEntity(playerUUID.toString()));
        }
    }
    
}
