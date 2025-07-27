package net.cmr.alchemycompany.troops;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import net.cmr.alchemycompany.GameScreen;
import net.cmr.alchemycompany.Player;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.Sprites.SpriteType;
import net.cmr.alchemycompany.World;

public class Scout extends Troop {

    public Scout(Player player, World world, int x, int y) {
        super(player, world, x, y);
    }

    @Override
    public int getTotalMoves() {
        return 4;
    }

    @Override
    public float getMaxHealth() {
        return 30;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (this.equals(player.selectedTroop)) {
            batch.draw(Sprites.getTexture(SpriteType.HIGHLIGHTED_TROOP), x * GameScreen.TILE_SIZE, y * GameScreen.TILE_SIZE, GameScreen.TILE_SIZE, GameScreen.TILE_SIZE);
        }
        batch.draw(Sprites.getTexture(SpriteType.SCOUT), x * GameScreen.TILE_SIZE, y * GameScreen.TILE_SIZE, GameScreen.TILE_SIZE, GameScreen.TILE_SIZE);
    }

    @Override
    public FightInformation getAttack() {
        return new FightInformation(10, AttackType.NORMAL);
    }

    @Override
    public FightInformation getDefense(FightInformation attack) {
        return new FightInformation(5, AttackType.NORMAL);
    }
    
}
