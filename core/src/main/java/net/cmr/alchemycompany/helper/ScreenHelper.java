package net.cmr.alchemycompany.helper;

import java.util.UUID;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.screen.GameScreen;

public abstract class ScreenHelper {
    
    public UUID playerUUID;
    public GameScreen screen;
    public GameManager gameManager;
    public Skin skin;

    public ScreenHelper(GameScreen screen, GameManager gameManager, UUID playerUUID) {
        this.screen = screen;
        this.gameManager = gameManager;
        this.playerUUID = playerUUID;
        this.skin = Sprites.getSkin();
    }

}
