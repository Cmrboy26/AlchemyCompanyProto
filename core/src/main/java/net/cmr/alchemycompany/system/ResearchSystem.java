package net.cmr.alchemycompany.system;

import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
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
            OwnerComponent oc = entity.getComponent(OwnerComponent.class);
            ResearchActionComponent rac = entity.getComponent(ResearchActionComponent.class);
            ResearchManagementComponent rmc = getPlayerResearchManager(oc.playerID);
            Technology tech = Registry.getInstance().getRegistry(Technology.class).get(rac.getResearchId());
            if (tech == null) {
                return; // Invalid technology
            }

            if (rmc.getCurrentResearch() != null && rmc.getCurrentResearch().equals(tech)) {
                // Already researching this technology, ignore
                return;
            }
            if (rmc.hasResearched(tech.getId())) {
                // Already researched this technology, ignore
                return;
            }
            rmc.stopResearch();
            rmc.startResearch(tech.getId());
            engine.changedEntity(entity);
        });
    }

    public ResearchManagementComponent getPlayerResearchManager(String playerID) {
        for (Entity entity : engine.getEntities(Family.all(ResearchManagementComponent.class, OwnerComponent.class))) {
            ResearchManagementComponent rmc = entity.getComponent(ResearchManagementComponent.class);
            OwnerComponent oc = entity.getComponent(OwnerComponent.class);
            if (oc.playerID.equals(playerID)) {
                return rmc;
            }
        }
        throw new IllegalStateException("No ResearchManagementComponent found for player " + playerID);
    }

    public boolean hasTechnology(String playerId, String technologyId) {
        ResearchManagementComponent rmc = getPlayerResearchManager(playerId);
        return rmc.hasResearched(technologyId);
    }
    
}
