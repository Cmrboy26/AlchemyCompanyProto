package net.cmr.alchemycompany.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class UpdatingImage extends Image {

    UpdatingImageProvider provider;

    public UpdatingImage(UpdatingImageProvider provider) {
        super(provider.get());
        this.provider = provider;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        setDrawable(provider.get());
    }

    @FunctionalInterface
    public static interface UpdatingImageProvider {
        public Drawable get();
    }

}
