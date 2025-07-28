package net.cmr.alchemycompany.troops;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import net.cmr.alchemycompany.Player;
import net.cmr.alchemycompany.World;
import net.cmr.alchemycompany.Building;
import net.cmr.alchemycompany.Building.BuildingContext;
import net.cmr.alchemycompany.Resources.Resource;

public abstract class Troop implements HealthHolder {
    
    Player player;
    World world;
    float health;
    int x, y;
    int movesRemaining;
    boolean attacked = false;

    public Troop(BuildingContext context) {
        this.player = context.player;
        this.world = context.world;
        this.x = context.x;
        this.y = context.y;
        this.movesRemaining = getTotalMoves();
        this.health = getMaxHealth();
        this.attacked = false;
    }

    @FunctionalInterface
    public static interface TroopConstructorFunction {
        Troop apply(BuildingContext context);
    }

    public enum AttackType {
        NORMAL,
        FIRE,
        CORROSION,
        LASER,
        ;
    }

    public interface EquipmentHolder {

        void equip(Equipment e);
        void unequip();
        Equipment getEquipment();
        public default void useEquipment() {
            if (getEquipment() != null) {
                boolean consumed = getEquipment().useEquipment();
                if (consumed) {
                    unequip();
                }
            }
        }

    }

    public static class FightInformation {
        
        public static final FightInformation NO_ATTACK = new FightInformation(0, AttackType.NORMAL);
        public AttackType attackType;
        public float strength;

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
        }
    }

    public void decrementMove() {
        this.movesRemaining--;
    }

    public void setMoveCounter(int counter) {
        this.movesRemaining = counter;
    }

    public float getMovesRemaining() {
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

    @Override
    public boolean processAttack(FightInformation attack, HealthHolder attacker) {
        health -= attack.getStrength();
        if (attacker instanceof EquipmentHolder) {
            EquipmentHolder eqh = (EquipmentHolder) attacker;
            eqh.useEquipment();
        }
        if (health <= 0) {
            world.getTroops().remove(this);
            if (player.selectedTroop != null && player.selectedTroop.equals(this)) {
                player.selectedTroop = null;
            }
            return true;
        }
        return false;
    }

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

    public Table onClick(Skin skin) {
        Table table = new Table(skin);

        table.add("Owner: " + player.toString()).fillX().pad(10).colspan(1);
        table.add("Name: " + getClass().getSimpleName()).fillX().pad(10).colspan(1).row();

        Label healthLabel = new Label("Health: "+health+"/"+getMaxHealth(), skin) {
            @Override
            public void act(float delta) {
                setText("Health: "+health+"/"+getMaxHealth());
                super.act(delta);
            }
        };
        table.add(healthLabel).growX().pad(10).colspan(1);
        Label movesLabel = new Label("Moves left: ("+movesRemaining+"/"+getTotalMoves()+")", skin) {
            @Override
            public void act(float delta) {
                setText("Moves left: ("+movesRemaining+"/"+getTotalMoves()+")");
                super.act(delta);
            }
        };
        table.add(movesLabel).growX().pad(10).colspan(1).row();

        if (this instanceof EquipmentHolder) {
            final EquipmentHolder eqh = (EquipmentHolder) this;
            Label equipmentLabel = new Label("Equipment: "+eqh.getEquipment(), skin)  {
                @Override
                public void act(float delta) {
                    setText("Equipment: "+eqh.getEquipment());
                    super.act(delta);
                }
            };
            table.add(equipmentLabel).growX().pad(10).colspan(2).row();
            Label equipmentUses = new Label("Equipment Uses: 100", skin)  {
                @Override
                public void act(float delta) {
                    if (eqh.getEquipment() == null) {
                        setVisible(false);
                    } else {
                        setVisible(true);
                        setText("Equipment Uses: "+eqh.getEquipment().uses);
                    }

                    super.act(delta);
                }
            };
            table.add(equipmentUses).growX().padLeft(10).colspan(1);
            Label equipmentReactionType = new Label("Equipment Reaction: "+AttackType.CORROSION, skin)  {
                @Override
                public void act(float delta) {
                    if (eqh.getEquipment() == null) {
                        setVisible(false);
                    } else {
                        setVisible(true);
                        if (eqh.getEquipment() instanceof ReactionVessel) {
                            ReactionVessel vessel = (ReactionVessel) eqh.getEquipment();
                            AttackType type = ReactionVessel.calculateAttackResult(vessel.primaryResource, vessel.secondaryResource);
                            setText("Equipment Reaction: "+type);
                        } else {
                            setText("");
                        }
                    }

                    super.act(delta);
                }
            };
            table.add(equipmentReactionType).growX().padLeft(10).colspan(1).row();
            SelectBox<String> equipmentSelector = new SelectBox<>(skin);

            Equipment[] availableEquipment = new Equipment[] {
                null,
                new ReactionVessel(3, Resource.WITCH_EYE, Resource.CRYSTAL),
                new ReactionVessel(3, Resource.SULFURIC_ACID, Resource.CRYSTAL),
                new ReactionVessel(3, Resource.SULFURIC_ACID, Resource.SULFURIC_ACID)
            };
            String[] stringEquivelents = new String[availableEquipment.length];
            for (int i = 0; i < availableEquipment.length; i++) {
                Equipment e = availableEquipment[i];
                if (e == null) {
                    stringEquivelents[i] = "None";
                } else {
                    stringEquivelents[i] = e.toString();
                }
            }

            equipmentSelector.setItems(stringEquivelents);
            equipmentSelector.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    eqh.equip(availableEquipment[equipmentSelector.getSelectedIndex()]);
                }
            });
            table.add(equipmentSelector).growX().pad(10).colspan(2).row();
            
        }

        return table;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"#"+((hashCode()+"").substring(6));
    }

}
