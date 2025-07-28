package net.cmr.alchemycompany.troops;

import net.cmr.alchemycompany.troops.Troop.EquipmentHolder;
import net.cmr.alchemycompany.troops.Troop.FightInformation;

public interface HealthHolder {
    
    public float getHealth();
    public float getMaxHealth();

    public abstract FightInformation getDefense(FightInformation attack, HealthHolder attacker);
    public abstract FightInformation getAttack(HealthHolder defender);
    
    public abstract boolean processAttack(FightInformation attack, HealthHolder attacker);
    
    public default FightInformation modifyFightWithEquipment(FightInformation defaultFight, HealthHolder opponent, boolean attack) {
        if (this instanceof EquipmentHolder) {
            EquipmentHolder eqh = (EquipmentHolder) this;
            if (eqh.getEquipment() != null) {
                if (attack) {
                    eqh.getEquipment().modifyAttack(defaultFight, opponent);
                } else {
                    eqh.getEquipment().modifyDefense(defaultFight, opponent);
                }
            }
        }
        return defaultFight;
    }

}
