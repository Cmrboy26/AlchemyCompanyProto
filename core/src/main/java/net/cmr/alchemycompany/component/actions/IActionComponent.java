package net.cmr.alchemycompany.component.actions;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.Family;

public interface IActionComponent {

    public static void processActionEntities(Engine engine, Class<? extends Component> actionClass, Consumer<Entity> action) {
        Family family = Family.all(PlayerActionComponent.class, actionClass);
        Set<Entity> entities = engine.getEntities(family);
        for (Entity entity : entities) {
            action.accept(entity);
        }
        for (Entity entity : entities) {
            engine.removeEntity(entity);
        }
    }

    public static <T extends Component> void processActionEntities(Engine engine, Class<T> actionClass, ActionConsumer<T> action) {
        Family family = Family.all(PlayerActionComponent.class, actionClass);
        Set<Entity> entities = engine.getEntities(family);
        for (Entity entity : entities) {
            action.accept(entity, entity.getComponent(PlayerActionComponent.class), entity.getComponent(actionClass));
        }
        for (Entity entity : entities) {
            engine.removeEntity(entity);
        }
    }

    @FunctionalInterface
    public static interface ActionConsumer<A extends Component> {
        void accept(Entity e, PlayerActionComponent p, A a);
    }

}
