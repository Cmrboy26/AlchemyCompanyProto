package net.cmr.alchemycompany.troops;

import net.cmr.alchemycompany.troops.Troop.FightInformation;

public abstract class Equipment {

    public static final int UNLIMITED_USES = 100;

    int uses;

    public Equipment(int uses) {
        this.uses = uses;
    }

    public abstract void modifyAttack(FightInformation attack, HealthHolder defender);
    public abstract void modifyDefense(FightInformation defense, HealthHolder attacker);
    public boolean useEquipment() {
        if (uses != UNLIMITED_USES) {
            uses--;
        }
        return uses <= 0;
    }

}
