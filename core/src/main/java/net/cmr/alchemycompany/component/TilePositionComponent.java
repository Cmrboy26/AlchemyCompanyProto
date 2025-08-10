package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class TilePositionComponent extends Component {
    
    public int tileX = 0;
    public int tileY = 0;

    public TilePositionComponent() {}

    public TilePositionComponent(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    @Override
    public void write(Json json) {
        json.writeField(this, "tileX");
        json.writeField(this, "tileY");
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        this.tileX = json.readValue("tileX", Integer.class, jsonData);
        this.tileY = json.readValue("tileY", Integer.class, jsonData);
    }

}
