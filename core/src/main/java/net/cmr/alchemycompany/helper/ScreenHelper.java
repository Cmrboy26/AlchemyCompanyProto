package net.cmr.alchemycompany.helper;

import java.util.UUID;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.GameScreen;
import net.cmr.alchemycompany.Sprites;

public abstract class ScreenHelper {
    
    protected UUID playerUUID;
    protected GameScreen screen;
    protected GameManager gameManager;
    protected Skin skin;

    public ScreenHelper(GameScreen screen, GameManager gameManager, UUID playerUUID) {
        this.screen = screen;
        this.gameManager = gameManager;
        this.playerUUID = playerUUID;
        this.skin = Sprites.getSkin();
    }

}
