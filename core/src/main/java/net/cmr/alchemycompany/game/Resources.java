package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import net.cmr.alchemycompany.Sprites;

public class Resources {

    public static HashMap<String, Float> singleItem(String resourceId, Float amount) {
        HashMap<String, Float> map = new HashMap<>();
        map.put(resourceId, amount);
        return map;
    }

    public static HashMap<String, Float> allItems(Float amount, boolean includePerTurn) {
        HashMap<String, Float> map = new HashMap<>();
        for (Resource resource : Registry.getInstance().getRegistry(Resource.class).values()) {
            if (includePerTurn || !resource.isPerTurnResource()) {
                map.put(resource.getId(), amount);
            }
        }
        return map;
    }

    private Map<Resource, Float> resources;
    public Resources() {
        resources = new HashMap<>();
    }

    public static Resources start() {
        return new Resources();
    }
    public Resources add(Resource r, Float amount) {
        resources.put(r, resources.getOrDefault(r, 0f) + amount);
        return this;
    }
    public Map<Resource, Float> build() {
        return resources;
    }

    // TODO: display max storage here
    public static Table createResourceTable(Resource resource, float productionAmount, float storageAmount) {
        Skin skin = Sprites.getSkin();
        Table resourceInfoTable = new Table(skin);
        // System.out.println(productionAmount + ": "+resource.getName());
        if (productionAmount == 0 && storageAmount == 0) {
            return resourceInfoTable;
        }
        String sign = productionAmount > 0 ? "+" : "";

        Label storageLabel = null;
        Label generationLabel = new Label(sign + productionAmount, skin);
        if (!resource.isPerTurnResource()) {
            storageLabel = new Label(storageAmount+"", skin);
            resourceInfoTable.add(storageLabel).spaceRight(2);
        }
        generationLabel.setFontScale(0.7f);
        resourceInfoTable.add(new Image(Sprites.getSprite(resource.getIcon()))).size(12).pad(2).spaceRight(2);
        resourceInfoTable.add(generationLabel);
        return resourceInfoTable;
    }

}

