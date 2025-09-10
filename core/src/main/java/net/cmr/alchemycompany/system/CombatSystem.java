package net.cmr.alchemycompany.system;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import net.cmr.alchemycompany.ITurnSystem;
import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.AttackComponent;
import net.cmr.alchemycompany.component.AttackCooldownComponent;
import net.cmr.alchemycompany.component.Component;
import net.cmr.alchemycompany.component.DefenseComponent;
import net.cmr.alchemycompany.component.HealthComponent;
import net.cmr.alchemycompany.component.MovementComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.actions.AttackActionComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.game.Effects.AttackType;

public class CombatSystem extends EntitySystem implements IUpdateSystem, ITurnSystem {
    
    Family combatFamily;

    @Override
    public void addedToEngine(Engine engine) {
        combatFamily = Family.all(HealthComponent.class, OwnerComponent.class);
    }

    @Override
    public void update(float delta) {
        IActionComponent.processActionEntities(engine, AttackActionComponent.class, (entity, pac, aac) -> {
            Entity attackingEntity = engine.getEntity(aac.getEntityUUID());
            Entity defendingEntity = engine.getEntity(aac.getTargetUUID());
            if (attackingEntity == null || defendingEntity == null) {
                return;
            }
            if (!combatFamily.matches(attackingEntity) || !combatFamily.matches(defendingEntity)) {
                return;
            }
            OwnerComponent ocAE = attackingEntity.getComponent(OwnerComponent.class);
            OwnerComponent ocDE = defendingEntity.getComponent(OwnerComponent.class);
            if (!pac.playerUUID.toString().equals(ocAE.playerID) || pac.playerUUID.toString().equals(ocDE.playerID)) {
                return;
            }
            if (attackingEntity.hasComponent(AttackCooldownComponent.class)) {
                return;
            }

            // Entities verified. Make attack happen
            AttackSimulation simulation = AttackSimulation.calculate(attackingEntity, defendingEntity);
            simulation.apply(engine);
        });
    }

    @Override
    public void onTurn() {
        for (Entity entity : new HashSet<>(engine.getComponentMapper(AttackCooldownComponent.class))) {
            entity.removeComponent(AttackActionComponent.class, engine);
            engine.changedEntity(entity);
        }
    }

    @Override
    public int getTurnPriority() {
        return 0;
    }
    

    public static class AttackSimulation {

        Entity attackingEntity, defendingEntity;
        Set<Component> attackerModifiedComponents, defenderModifiedComponents;
        float attackerStrength, defenderStrength;

        private AttackSimulation(Entity attackingEntity, Set<Component> attackerModifiedComponents, Entity defendingEntity, Set<Component> defenderModifiedComponents, Float attackerStrength, Float defenderStrength) {
            this.attackingEntity = attackingEntity;
            this.defendingEntity = defendingEntity;
            this.attackerModifiedComponents = attackerModifiedComponents;
            this.defenderModifiedComponents = defenderModifiedComponents;
            this.attackerStrength = attackerStrength;
            this.defenderStrength = defenderStrength;
        }

        public void apply(Engine engine) {
            for (Component component : attackerModifiedComponents) {
                attackingEntity.setComponent(component, engine);
            }
            if (attackingEntity.getComponent(HealthComponent.class).health <= 0) {
                engine.removeEntity(attackingEntity);
            } else {
                if (attackingEntity.hasComponent(MovementComponent.class)) {
                    attackingEntity.getComponent(MovementComponent.class).movesRemaining = 0;
                }
                engine.changedEntity(attackingEntity);
            }
            for (Component component : defenderModifiedComponents) {
                defendingEntity.setComponent(component, engine);
            }
            if (defendingEntity.getComponent(HealthComponent.class).health <= 0) {
                engine.removeEntity(defendingEntity);
            } else {
                /*if (defendingEntity.hasComponent(MovementComponent.class)) {
                    defendingEntity.getComponent(MovementComponent.class).movesRemaining = 0;
                }*/
                engine.changedEntity(defendingEntity);
            }
        }

        public static AttackSimulation calculate(Entity attackingEntity, Entity defendingEntity) {
            Set<Component> attackerModifiedComponents = new HashSet<>();
            Set<Component> defenderModifiedComponents = new HashSet<>();
            HealthComponent mutableAttackerHealth = attackingEntity.getComponent(HealthComponent.class).cloneComponent(HealthComponent.class);
            HealthComponent mutableDefenderHealth = defendingEntity.getComponent(HealthComponent.class).cloneComponent(HealthComponent.class);
            attackerModifiedComponents.add(mutableAttackerHealth);
            defenderModifiedComponents.add(mutableDefenderHealth);
            attackerModifiedComponents.add(new AttackCooldownComponent());
            AttackComponent attackerAttackComponent = attackingEntity.getComponent(AttackComponent.class);
            AttackComponent defenderAttackComponent = defendingEntity.getComponent(AttackComponent.class);
            DefenseComponent attackerDefenseComponent = defendingEntity.getComponent(DefenseComponent.class);
            DefenseComponent defenderDefenseComponent = defendingEntity.getComponent(DefenseComponent.class);

            Function<Float, Float> attackScaleAsHealthFunction = (percentMaxHealth) -> {
                percentMaxHealth = Math.min(1, Math.max(0, percentMaxHealth));
                return (float) Math.pow(1.5d, 2d * (1 - Math.pow(percentMaxHealth, 0.15d)));
            };

            // Attacker fights defender
            AttackType attackerType = attackerAttackComponent.attackType;
            float attackerCalculatedDamage = attackerAttackComponent.baseAttack;
            if (defenderDefenseComponent != null) {
                attackerCalculatedDamage *= defenderDefenseComponent.typeMultipliers.getOrDefault(attackerType, 1f);
            }
            attackerCalculatedDamage *= attackScaleAsHealthFunction.apply(mutableAttackerHealth.health / mutableAttackerHealth.maxHealth);
            mutableDefenderHealth.health -= Math.max(attackerCalculatedDamage, 0);
            if (mutableDefenderHealth.health <= 0) {
                // If defender died, continue
                return new AttackSimulation(attackingEntity, attackerModifiedComponents, defendingEntity, defenderModifiedComponents, attackerCalculatedDamage, 0f);
            }
            
            // If defender can retaliate, attack back
            float defenderCalculatedDamage = 0;
            if (defenderAttackComponent != null) {
                AttackType defenderType = defenderAttackComponent.attackType;
                defenderCalculatedDamage = defenderAttackComponent.baseAttack;
                if (defenderDefenseComponent != null) {
                    attackerCalculatedDamage *= attackerDefenseComponent.typeMultipliers.getOrDefault(defenderType, 1f);
                }
                defenderCalculatedDamage *= attackScaleAsHealthFunction.apply(mutableDefenderHealth.health / mutableDefenderHealth.maxHealth);
                mutableAttackerHealth.health -= Math.max(defenderCalculatedDamage, 0);
                if (mutableAttackerHealth.health <= 0) {
                    // If attacker died
                }
            }

            return new AttackSimulation(attackingEntity, attackerModifiedComponents, defendingEntity, defenderModifiedComponents, attackerCalculatedDamage, defenderCalculatedDamage);
        }

    }

}
