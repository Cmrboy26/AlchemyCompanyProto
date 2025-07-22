package net.cmr.alchemycompany;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class AlchemyCompany extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}