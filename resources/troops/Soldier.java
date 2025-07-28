package net.cmr.alchemycompany.troops;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import net.cmr.alchemycompany.Building.BuildingContext;
import net.cmr.alchemycompany.GameScreen;
import net.cmr.alchemycompany.Player;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.troops.Troop.EquipmentHolder;
import net.cmr.alchemycompany.World;

public class Soldier extends Troop implements EquipmentHolder {

    Equipment equipment;

    public Soldier(BuildingContext context) {
        super(context);
    }

    @Override
    public int getTotalMoves() {
        return 2;
    }

    @Override
    public float getMaxHealth() {
        return 80;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (this.equals(player.selectedTroop)) {
            batch.draw(Sprites.getTexture(SpriteType.HIGHLIGHTED_TROOP), x * GameScreen.TILE_SIZE, y * GameScreen.TILE_SIZE, GameScreen.TILE_SIZE, GameScreen.TILE_SIZE);
        }
        batch.draw(Sprites.getTexture(SpriteType.SOLDIER), x * GameScreen.TILE_SIZE, y * GameScreen.TILE_SIZE, GameScreen.TILE_SIZE, GameScreen.TILE_SIZE);
    }

    @Override
    public FightInformation getAttack(HealthHolder defender) {
        return modifyFightWithEquipment(new FightInformation(20, AttackType.NORMAL), defender, true);
    }

    @Override
    public FightInformation getDefense(FightInformation attack, HealthHolder attacker) {
        return modifyFightWithEquipment(new FightInformation(15, AttackType.NORMAL), attacker, false);
    }

    @Override
    public void equip(Equipment e) {
        this.equipment = e;
    }

    @Override
    public void unequip() {
        this.equipment = null;
    }

    @Override
    public Equipment getEquipment() {
        return equipment;
    }
    
}
