package net.cmr.alchemycompany.ecs;

import java.io.Serializable;
import java.lang.reflect.Field;

public abstract class Component implements Serializable {
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("[");
        Field[] fields = getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            try {
                sb.append(fields[i].getName()).append(":").append(fields[i].get(this));
            } catch (IllegalAccessException e) {
                sb.append(fields[i].getName()).append(":<inaccessible>");
            }
            if (i < fields.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
