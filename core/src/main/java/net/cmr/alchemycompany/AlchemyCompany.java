package net.cmr.alchemycompany;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class AlchemyCompany extends Game {

    private SpriteBatch spriteBatch;
    private static AlchemyCompany instance;

    public AlchemyCompany() {
        instance = this;
    }

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        setScreen(new GameScreen());

        System.out.println(2 & 0x0001);
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