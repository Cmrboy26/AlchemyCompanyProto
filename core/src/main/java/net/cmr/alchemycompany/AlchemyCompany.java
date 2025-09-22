package net.cmr.alchemycompany;

import java.util.UUID;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.network.OnlineStream;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.screen.MainMenuScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class AlchemyCompany extends Game {

    private SpriteBatch spriteBatch;
    private static AlchemyCompany instance;

    public AlchemyCompany() {
        instance = this;
    }

    @Override
    public void create() {
        Registry.getInstance();
        Sprites.load();
        spriteBatch = new SpriteBatch();
        setScreen(new MainMenuScreen());
    }

    public SpriteBatch batch() {
        return spriteBatch;
    }

    public static AlchemyCompany getInstance() {
        return instance;
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        Sprites.dispose();
    }
}