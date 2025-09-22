package net.cmr.alchemycompany.component;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SelectableComponent extends Component {

    @Override
    public void write(Json json) {
        // No fields to write for this marker component
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        // No fields to read for this marker component
    }

}
