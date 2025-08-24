package net.cmr.alchemycompany.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.cmr.alchemycompany.component.SelectableComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;

public class SelectionSystem extends EntitySystem {

    private UUID selectedIdCache = null;
    private int sameTileClickCount = 0;

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
    }

    public void select(int tileX, int tileY) {
        Family eligibleEntities = Family.all(SelectableComponent.class, TilePositionComponent.class);
        Set<Entity> entities = engine.getEntities(eligibleEntities);
        List<Entity> locatedEntities = new ArrayList<>();
        
        if (selectedIdCache != null) {
            Entity entity = engine.getEntity(selectedIdCache);
            if (entity != null) {
                TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
                if (tileX == tpc.tileX && tileY == tpc.tileY) {
                    sameTileClickCount++;
                } else {
                    sameTileClickCount = 0;
                }
            }
        }

        for (Entity entity : entities) {
            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
            if (tileX == tpc.tileX && tileY == tpc.tileY) {
                locatedEntities.add(entity);
            }
        }
        Collections.sort(locatedEntities, Comparator.comparing(Entity::getID));

        if (locatedEntities.size() != 0) {
            int index = sameTileClickCount % locatedEntities.size();
            UUID selectedId = locatedEntities.get(index).getID();
            if (!selectedId.equals(selectedIdCache)) {
                selectedIdCache = locatedEntities.get(index).getID();
                System.out.println("Selected "+selectedIdCache);
            } else {
                deselect();
                System.out.println("Deselected");
            }
        } else {
            deselect();
            System.out.println("Deselected");
        }
    }

    public UUID getSelectedId() {
        return selectedIdCache;
    }

    public void deselect() {
        selectedIdCache = null;
    }

}
