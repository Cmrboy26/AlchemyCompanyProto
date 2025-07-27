package net.cmr.alchemycompany.troops;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import net.cmr.alchemycompany.Player;
import net.cmr.alchemycompany.World;

public abstract class Troop {
    
    Player player;
    World world;
    float health;
    int x, y, movesRemaining;
    boolean attacked = false;

    public Troop(Player player, World world, int x, int y) {
        this.player = player;
        this.world = world;
        this.x = x;
        this.y = y;
        this.movesRemaining = getTotalMoves();
        this.attacked = false;
    }

    public enum AttackType {
        NORMAL,
        FIRE,
        CORROSION,
        LASER,
        ;
    }

    public interface EquipmentHolder {
        // TODO: implement
    }

    public static class FightInformation {
        
        private AttackType attackType;
        private float strength;

        public FightInformation(float strength, AttackType attackType) {
            this.strength = strength;
            this.attackType = attackType;
        }

        public AttackType getAttackType() {
            return attackType;
        }

        public float getStrength() {
            return strength;
        }
        
    }

    public void moveTo(int x, int y) {
        x = Math.min(Math.max(x, 0), world.width - 1);
        y = Math.min(Math.max(y, 0), world.height - 1);
        while (movesRemaining > 0 && (this.x != x || this.y != y)) {
            int newX = this.x + (int) Math.signum(x - this.x); 
            int newY = this.y + (int) Math.signum(y - this.y);
            if (overlapsWithTroop(world, newX, newY)) {
                break;
            }
            this.x = newX;
            this.y = newY;
            decrementMove();
            System.out.println(movesRemaining + ": " + getX() + ", "+getY());
        }
        System.out.println("FINISHED");
    }

    public void decrementMove() {
        this.movesRemaining--;
    }

    public void setMoveCounter(int counter) {
        this.movesRemaining = counter;
    }

    public int getMovesRemaining() {
        return movesRemaining;
    }

    public void resetMoves() {
        this.movesRemaining = getTotalMoves();
        this.attacked = false;
    }

    public void setAttacked(boolean attacked) {
        this.attacked = attacked;
    }

    public boolean hasAttacked() {
        return attacked;
    }

    public boolean inRangeOfAttack(int x, int y) {
        int dist = Math.max(Math.abs(getX() - x), Math.abs(getY() - y));
        return dist <= getAttackRange();
    }

    public static boolean overlapsWithTroop(World world, int x, int y) {
        for (Troop troop : world.getTroops()) {
            if (troop.getX() == x && troop.getY() == y) {
                return true;
            }
        }
        return false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    
    public abstract int getTotalMoves();
    public abstract float getMaxHealth();
    public abstract void render(SpriteBatch batch);

    public float getHealth() {
        return health;
    }
    
    /**
     * 1 = right next to troop
     * 2 = 1 tile away
     * etc
     */
    public int getAttackRange() {
        return 1;
    }

    public abstract FightInformation getAttack();
    public abstract FightInformation getDefense(FightInformation attack);

}
