package net.cmr.alchemycompany.screen.menus;

import java.util.function.Consumer;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.helper.ScreenHelper;

public abstract class GameMenu extends Table {
    
    public final Skin skin;
    public final ScreenHelper screenHelper;
    public final String name;
    public final Label titleLabel;
    public final TextButton closeButton;
    private Consumer<GameMenu> onCloseRunnable = null;

    public GameMenu(int align, ScreenHelper screenHelper, String name) {
        super(Sprites.getSkin());
        this.screenHelper = screenHelper;
        this.skin = getSkin();
        this.name = name;
        this.setBackground("window");
        this.align(align);
        this.pad(10);
        
        Table topBar = new Table();
        this.add(topBar).expandX().fillX().row();

        titleLabel = new Label(name, skin);
        titleLabel.setFontScale(1.5f);
        topBar.add(titleLabel).left().expandX();
        closeButton = new TextButton("X", skin);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                menuClosed();
            }
        });
        topBar.add(closeButton).height(16).width(48).right();

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
    public final void menuClosed() {
        if (onCloseRunnable != null) {
            onCloseRunnable.accept(this);
        }
        onClose();
    }
    public final void setOnClose(Consumer<GameMenu> consumer) {
        this.onCloseRunnable = consumer;
    }
    /**
     * Additional close method for subclasses to override
     */
    public void onClose() {

    }

}
