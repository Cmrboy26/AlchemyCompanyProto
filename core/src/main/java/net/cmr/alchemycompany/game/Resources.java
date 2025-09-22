package net.cmr.alchemycompany.game;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Null;

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
    /*
     * public static Table createResourceTable(Resource resource, float
     * productionAmount, float storageAmount) {
     * Skin skin = Sprites.getSkin();
     * Table resourceInfoTable = new Table(skin);
     * // System.out.println(productionAmount + ": "+resource.getName());
     * if (productionAmount == 0 && storageAmount == 0) {
     * return resourceInfoTable;
     * }
     * String sign = productionAmount > 0 ? "+" : "";
     * 
     * Label storageLabel = null;
     * Label generationLabel = new Label(sign + productionAmount, skin);
     * if (!resource.isPerTurnResource()) {
     * storageLabel = new Label(storageAmount+"", skin);
     * resourceInfoTable.add(storageLabel).spaceRight(2);
     * }
     * generationLabel.setFontScale(0.7f);
     * resourceInfoTable.add(new
     * Image(Sprites.getSprite(resource.getIcon()))).size(12).pad(2).spaceRight(2);
     * resourceInfoTable.add(generationLabel);
     * return resourceInfoTable;
     * }
     */

    public static Table createResourceTable(String resourceId, float iconSize, @Null Float storageAmount, @Null Float maxStorageAmount, @Null Float productionAmount, boolean displaySign) {
        Table resourceTable = new Table();
        Resource resource = Registry.getInstance().getRegistry(Resource.class).get(resourceId);
        if (resource == null) return resourceTable;

        Image icon = new Image(Sprites.getSprite(resource.getIcon()));
        icon.setName("image");
        icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        icon.setSize(iconSize, iconSize);
        resourceTable.add(icon).size(iconSize);

        
        String storageString = "";
        if (storageAmount != null) {
            String sign = "";
            if (displaySign) {
                if (storageAmount > 0) sign = "+";
                else if (storageAmount < 0) sign = "-";
            }
            storageString = sign + String.format("%.0f", Math.abs(storageAmount));
        }

        if (maxStorageAmount != null && maxStorageAmount >= 0) {
            storageString += " / "+String.format("%.0f", maxStorageAmount);
        }
        if (!storageString.isEmpty()) {
            resourceTable.add(new Label(storageString, Sprites.getSkin())).padLeft(2).padRight(4);
        }
        if (productionAmount != null) {
            String sign = "";
            if (productionAmount > 0) sign = "+";
            else if (productionAmount < 0) sign = "-";
            Label productionLabel = new Label(sign + String.format("%.0f", Math.abs(productionAmount)), Sprites.getSkin());
            productionLabel.setName("production");
            resourceTable.add(productionLabel).padLeft(2);
        }
        
        Table tooltipTable = new Table(Sprites.getSkin());
        tooltipTable.setBackground(Sprites.getSkin().getDrawable("window"));
        tooltipTable.pad(4);
        Label nameLabel = new Label(resource.getName(), Sprites.getSkin());
        nameLabel.setFontScale(0.75f);
        tooltipTable.add(nameLabel).pad(2).row();
        Tooltip<Table> tooltip = new Tooltip<Table>(tooltipTable);
        tooltip.setInstant(true);
        TooltipManager.getInstance().animations = false;
        resourceTable.addListener(tooltip);

        resourceTable.pack();
        
        return resourceTable;
    }

}   