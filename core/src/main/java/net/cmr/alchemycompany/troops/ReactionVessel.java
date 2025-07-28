package net.cmr.alchemycompany.troops;

import java.util.function.Function;

import net.cmr.alchemycompany.Building;
import net.cmr.alchemycompany.Resources.Resource;
import net.cmr.alchemycompany.troops.Troop.AttackType;
import net.cmr.alchemycompany.troops.Troop.FightInformation;

public class ReactionVessel extends Equipment {
    
    Resource primaryResource, secondaryResource;

    public ReactionVessel(int uses, Resource primaryResource, Resource secondaryResource) {
        super(3);
        this.uses = uses;
        this.primaryResource = primaryResource;
        this.secondaryResource = secondaryResource;
    }

    public static AttackType calculateAttackResult(final Resource primaryResource, final Resource secondaryResource) {
        Function<Resource, Boolean> eitherEquals = (compare) -> {
            return primaryResource == compare || secondaryResource == compare;
        };

        if (eitherEquals.apply(Resource.CRYSTAL) && eitherEquals.apply(Resource.WITCH_EYE)) {
            return AttackType.LASER;
        }
        if (eitherEquals.apply(Resource.CRYSTAL) && eitherEquals.apply(Resource.SULFURIC_ACID)) {
            return AttackType.FIRE;
        }
        if (primaryResource == Resource.SULFURIC_ACID && secondaryResource == Resource.SULFURIC_ACID) {
            return AttackType.CORROSION;
        }

        return null;
    }

    @Override
    public void modifyAttack(FightInformation attack, HealthHolder defender) {
        AttackType attackType = calculateAttackResult(primaryResource, secondaryResource);
        if (attackType != null) {
            attack.attackType = attackType;
            if (attack.attackType == AttackType.FIRE) {
                attack.strength *= 1.5f;
            }
            if (defender instanceof Building && attackType == AttackType.CORROSION) {
                attack.strength *= 2f;
            }
            if (defender instanceof Troop && attackType == AttackType.LASER) {
                attack.strength *= 1.5f;
            }
        }
    }

    @Override
    public void modifyDefense(FightInformation defense, HealthHolder attacker) {
        AttackType attackType = calculateAttackResult(primaryResource, secondaryResource);
        if (attackType != null) {
            defense.attackType = attackType;
            if (defense.attackType == AttackType.FIRE) {
                defense.strength *= 1.5f;
            }
            if (attacker instanceof Building && attackType == AttackType.CORROSION) {
                defense.strength *= 2f;
            }
            if (attacker instanceof Troop && attackType == AttackType.LASER) {
                defense.strength *= 1.5f;
            }
        }
    }

    @Override
    public String toString() {
        return "Reaction Vessel ["+primaryResource+" + "+secondaryResource+"]";
    }

}
