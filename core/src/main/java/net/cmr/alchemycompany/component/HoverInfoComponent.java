package net.cmr.alchemycompany.component;

import net.cmr.alchemycompany.ecs.Component;

public class HoverInfoComponent extends Component {
    
    public String hoverText;

    public HoverInfoComponent(String hoverText) {
        this.hoverText = hoverText;
    }    

}
