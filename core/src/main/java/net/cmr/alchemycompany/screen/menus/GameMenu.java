package net.cmr.alchemycompany.screen.menus;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.helper.ScreenHelper;

public abstract class GameMenu extends Table {
    
    public final Skin skin;
    public final ScreenHelper screenHelper;

    public GameMenu(int align, ScreenHelper screenHelper) {
        super(Sprites.getSkin());
        this.screenHelper = screenHelper;
        this.skin = getSkin();
        this.setBackground("window");
        this.align(align);
        this.pad(10);
        construct();
    }
    
    public abstract void construct();
    public void refresh() {
        this.clearChildren();
        construct();
    }
    protected void addContinuousUpdate(Runnable runnable) {
        addContinuousUpdate(this, runnable);
    }
    public static void addContinuousUpdate(Actor actor, Runnable runnable) {
        actor.addAction(Actions.forever(Actions.run(runnable)));
    }

}
