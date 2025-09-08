package net.cmr.alchemycompany.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

import net.cmr.alchemycompany.ecs.Entity;

public class EntityReader<T> {
    
    private Map<T, Entity> entitiesCache = null;

    public EntityReader() {

    }

    public void readEntities(FileHandle jsonFile, Function<Entity, T> entityMapper) {
        Json json = new Json();
        JsonReader reader = new JsonReader();
        JsonValue values = reader.parse(jsonFile);
        Map<T, Entity> entityMap = new HashMap<>();
        for (JsonValue entry = values.child; entry != null; entry = entry.next) {
            Entity entity = json.fromJson(Entity.class, entry.toJson(OutputType.json));
            entityMap.put(entityMapper.apply(entity), entity);
        }
        this.entitiesCache = entityMap;
    }

    public Map<T, Entity> cloneEntityMap() {
        // Clone the entities in the entities cache
        Map<T, Entity> entityMap = new HashMap<>();
        for (Entry<T, Entity> entry : entitiesCache.entrySet()) {
            entityMap.put(entry.getKey(), entry.getValue().cloneEntity(true));
        }
        return entityMap;
    }

    public Entity getEntity(T key) {
        Entity entity = entitiesCache.get(key);
        if (entity == null) {
            return null;
        }
        return entity.cloneEntity(true);
    }

}
