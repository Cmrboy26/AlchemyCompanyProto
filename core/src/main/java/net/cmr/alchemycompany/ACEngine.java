package net.cmr.alchemycompany;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.system.RenderSystem;

public class ACEngine extends Engine {
    
    List<EntitySystem> renderSystems = new ArrayList<>();
    List<EntitySystem> updateSystems = new ArrayList<>();

    public void update(float delta) {
        for (EntitySystem system : updateSystems) {
            ((IUpdateSystem) system).update(delta);
        }
    }

    public void render(SpriteBatch batch, float delta) {
        batch.begin();
        for (EntitySystem system : renderSystems) {
            ((RenderSystem) system).render(batch, delta);
        }
        batch.end();
    }

    @Override
    public void registerSystem(EntitySystem system) {
        super.registerSystem(system);
        if (system instanceof RenderSystem) {
            renderSystems.add(system);
        }
        if (system instanceof IUpdateSystem) {
            renderSystems.add(system);
        }
    }

    @Override
    public void unregisterSystem(EntitySystem system) {
        super.unregisterSystem(system);
        if (system instanceof RenderSystem) {
            renderSystems.remove(system);
        }
        if (system instanceof IUpdateSystem) {
            renderSystems.remove(system);
        }
    }

}
