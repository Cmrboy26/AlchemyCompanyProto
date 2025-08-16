package net.cmr.alchemycompany;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.world.World;

public class ACEngine extends Engine {

    List<EntitySystem> renderSystems = new ArrayList<>();
    List<EntitySystem> updateSystems = new ArrayList<>();
    private World world;

    public void setWorld(World world) {
        this.world = world;
    }

    public void update(float delta) {
        for (EntitySystem system : updateSystems) {
            ((IUpdateSystem) system).update(delta);
        }
    }

    public void render(UUID playerUUID, SpriteBatch batch, float delta) {
        batch.begin();
        for (EntitySystem system : renderSystems) {
            ((RenderSystem) system).render(playerUUID, batch, delta);
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
            updateSystems.add(system);
        }
    }

    @Override
    public void unregisterSystem(EntitySystem system) {
        super.unregisterSystem(system);
        if (system instanceof RenderSystem) {
            renderSystems.remove(system);
        }
        if (system instanceof IUpdateSystem) {
            updateSystems.remove(system);
        }
    }

    public World getWorld() {
        return world;
    }

}
