package net.cmr.alchemycompany.component.actions;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import net.cmr.alchemycompany.component.Component;

public class TurnActionComponent extends Component implements IActionComponent {

    public boolean turnFinished;

    public TurnActionComponent() { }
    public TurnActionComponent(boolean turnFinished) {
        this.turnFinished = turnFinished;
    }

    @Override
    public void write(Json json) {
        json.writeValue("turnFinished", turnFinished);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        turnFinished = jsonData.getBoolean("turnFinished", false);
    }

}
