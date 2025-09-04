package net.cmr.alchemycompany.component.actions;

import java.util.Set;
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

}
