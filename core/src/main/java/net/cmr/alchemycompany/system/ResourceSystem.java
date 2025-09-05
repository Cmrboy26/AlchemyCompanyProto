package net.cmr.alchemycompany.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Null;

import net.cmr.alchemycompany.ITurnSystem;
import net.cmr.alchemycompany.Sprites;
import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.ResearchManagementComponent;
import net.cmr.alchemycompany.component.StorageComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Registry.ResourceFilter;
import net.cmr.alchemycompany.screen.GameScreen;
import net.cmr.alchemycompany.game.Resource;

public class ResourceSystem extends EntitySystem implements ITurnSystem {

    private Map<UUID, Map<String, Float>> cachedGenerationPerSecond = new HashMap<>();
    private Map<UUID, Map<String, Float>> cachedStoredResources = new HashMap<>();
    private Map<UUID, List<Entity>> activeEntities = new HashMap<>();
    private final @Null GameScreen playerScreen;

    public ResourceSystem() {
        this.playerScreen = null;
    }
    public ResourceSystem(GameScreen screen) {
        this.playerScreen = screen;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }

    public void calculateTurn(boolean simulate) {
        Set<UUID> players = new HashSet<>();
        if (playerScreen != null) {
            players.add(getLocalPlayerUUID());
        } else {
            Set<Entity> ownedEntities = engine.getComponentMapper(OwnerComponent.class);
            for (Entity e : ownedEntities) {
                OwnerComponent owner = e.getComponent(OwnerComponent.class);
                if (owner != null && owner.playerID != null) {
                    try {
                        players.add(UUID.fromString(owner.playerID));
                    } catch (IllegalArgumentException ex) {
                        System.err.println("Invalid UUID string in OwnerComponent: " + owner.playerID);
                    }
                }
            }
        }

        for (UUID playerUUID : players) {
            calculateTurn(playerUUID, simulate);
        }
    }

    private void calculateTurn(UUID playerUUID, boolean simulate) {
        Map<String, Float> generationPerSecond = new HashMap<>();
        Map<String, Float> trueResourcesInStorage = new HashMap<>();
        Map<String, Float> calculatedStoredResources = new HashMap<>();
        Map<String, Float> calculatedTotalStorageCapacity = new HashMap<>();

        List<Entity> activeEntities = calculateBuildingOutput(playerUUID, trueResourcesInStorage, calculatedStoredResources, calculatedTotalStorageCapacity);

        for (Resource resource : Registry.getResourceValues(ResourceFilter.OMIT_PER_TURN).values()) {
            float inStorage = trueResourcesInStorage.getOrDefault(resource.getId(), 0f);
            float afterTurn = calculatedStoredResources.getOrDefault(resource.getId(), 0f);
            afterTurn = Math.min(afterTurn, calculatedTotalStorageCapacity.getOrDefault(resource.getId(), 0f));
            float rps = afterTurn - inStorage;
            generationPerSecond.put(resource.getId(), rps);
        }
        for (Resource resource : Registry.getResourceValues(ResourceFilter.PER_TURN).values()) {
            float inStorage = trueResourcesInStorage.getOrDefault(resource.getId(), 0f);
            float afterTurn = calculatedStoredResources.getOrDefault(resource.getId(), 0f);
            float rps = afterTurn - inStorage;
            generationPerSecond.put(resource.getId(), rps);
        }

        Set<Entity> storageBuildings = engine.getEntities(Family.all(StorageComponent.class, OwnerComponent.class));
        if (!simulate) {
            // Put calulated resources into storage components
            for (Entity building : activeEntities) {
                // TODO: add active display
                //building.setActive();
            }

            ResearchSystem researchSystem = engine.getSystem(ResearchSystem.class);
            if (researchSystem != null) {
                researchSystem.consumeAvailableResources(playerUUID, calculatedStoredResources);
            }

            // TODO: use resources that are per turn (like science)
            for (Resource perTurnResources : Registry.getResourceValues(ResourceFilter.PER_TURN).values()) {
                // Remove per turn resources from storage (they are not stored, just used)
                calculatedStoredResources.remove(perTurnResources.getId());
            }

            // Fill storages with remaining resources
            for (Entity building : storageBuildings) {
                StorageComponent storage = building.getComponent(StorageComponent.class);
                OwnerComponent owner = building.getComponent(OwnerComponent.class);
                if (owner == null || owner.playerID == null || !owner.playerID.equals(playerUUID.toString())) {
                    continue; // Not owned by this player
                }

                for (String resourceID : storage.getMaxStorage().keySet()) {
                    storage.consumeAmount(resourceID, storage.getAmountStored(resourceID));
                    float toAdd = calculatedStoredResources.getOrDefault(resourceID, 0f);
                    float remainingAfterAdd = storage.addAmount(resourceID, toAdd); 
                    calculatedStoredResources.put(resourceID, remainingAfterAdd);
                    //displayStoredResources.put(resource, displayStoredResources.getOrDefault(resource, 0f) + storage.getAmountStored(resource));
                }
            }

            Set<Entity> buildingsToBroadcast = new HashSet<>();
            buildingsToBroadcast.addAll(activeEntities);
            buildingsToBroadcast.addAll(storageBuildings);
            for (Entity building : buildingsToBroadcast) {
                engine.changedEntity(building);
            }
        }

        trueResourcesInStorage.clear();
        for (Entity building : storageBuildings) {
            StorageComponent storage = building.getComponent(StorageComponent.class);
            OwnerComponent owner = building.getComponent(OwnerComponent.class);
            if (owner == null || owner.playerID == null || !owner.playerID.equals(playerUUID.toString())) {
                continue; // Not owned by this player
            }

            for (String resourceID : storage.getMaxStorage().keySet()) {
                float amountStored = storage.getAmountStored(resourceID);
                if (amountStored > 0) {
                    trueResourcesInStorage.put(resourceID,
                            trueResourcesInStorage.getOrDefault(resourceID, 0f) + amountStored);
                }
            }
        }

        /*System.out.println("Resource system calculating: "+playerUUID+"\t"+playerScreen);
        generationPerSecond.keySet().stream()
            .sorted()
            .filter((rid) -> { return generationPerSecond.get(rid) != 0 || trueResourcesInStorage.getOrDefault(rid, 0f) != 0; })
            .forEach(resourceId ->
            System.out.print(resourceId + ", " + generationPerSecond.get(resourceId) + "\t" + trueResourcesInStorage.getOrDefault(resourceId, 0f) + " / " + calculatedTotalStorageCapacity.getOrDefault(resourceId, 0f) + "\n")
            );
        System.out.println();*/

        this.cachedGenerationPerSecond.put(playerUUID, new HashMap<>(generationPerSecond));
        this.activeEntities.put(playerUUID, new ArrayList<>(activeEntities));
        this.cachedStoredResources.put(playerUUID, new HashMap<>(trueResourcesInStorage));
        //}
    }

    // TODO: things that only produce one thing should fill storage up to max. this doesnt happen with quartz for some reason???
    /**
     * @param resourcesInStorageOutput used for display in calculation
     * @param storedResourcesOutput amount of resources to be stored at the end of a turn
     * @param totalStorageCapacityOutput total amount of storage capacity (max holdable resources)
     * @return list of buildings that should be converted to active instead of idle
     */
    public List<Entity> calculateBuildingOutput(final UUID playerUUID, final Map<String, Float> resourcesInStorageOutput, final Map<String, Float> storedResourcesOutput, final Map<String, Float> totalStorageCapacityOutput) {
        // Produce and consume all resources
        List<Entity> buildingsToProcess = new ArrayList<>();
        // Use a set to avoid duplicates
        java.util.Set<Entity> uniqueEntities = new java.util.HashSet<>();
        uniqueEntities.addAll(engine.getComponentMapper(ProducerComponent.class));
        uniqueEntities.addAll(engine.getComponentMapper(ConsumerComponent.class));
        uniqueEntities.addAll(engine.getComponentMapper(StorageComponent.class));
        uniqueEntities.removeIf((e) -> {
            OwnerComponent owner = e.getComponent(OwnerComponent.class);
            return owner == null || owner.playerID == null || !owner.playerID.equals(playerUUID.toString());
        });
        buildingsToProcess.addAll(uniqueEntities);

        List<Entity> toBeActive = new ArrayList<>();
        boolean[] finalBuildingsProcessed = new boolean[buildingsToProcess.size()];
        Arrays.fill(finalBuildingsProcessed, false);

        // Sort buildings here if needed

        Callable<Boolean> everythingProcessed = () -> {
            for (int i = 0; i < finalBuildingsProcessed.length; i++) {
                if (!finalBuildingsProcessed[i]) {
                    return false;
                }
            }
            return true;
        };
        BiConsumer<String, Float> addResource = (r, f) -> {
            storedResourcesOutput.put(r, storedResourcesOutput.getOrDefault(r, 0f) + f);
        };
        try {
            do {
                boolean[] buildingsProcessed = new boolean[buildingsToProcess.size()];
                System.arraycopy(finalBuildingsProcessed, 0, buildingsProcessed, 0, buildingsProcessed.length);

                for (int i = 0; i < buildingsToProcess.size(); i++) {
                    Entity building = buildingsToProcess.get(i);

                    if (buildingsProcessed[i]) {
                        // Already processed, continue
                        continue;
                    }

                    /*if (!building.canFunction()) {
                        buildingsProcessed[i] = true;
                        continue;
                    }*/

                    boolean isProductionBuilding = building.hasComponent(ProducerComponent.class) && building.getComponent(ProducerComponent.class).production.size() != 0;
                    boolean isConsumptionBuilding = building.hasComponent(ConsumerComponent.class) && building.getComponent(ConsumerComponent.class).consumption.size() != 0;
                    boolean isStorageBuilding = building.hasComponent(StorageComponent.class);
                    boolean processed = false;

                    if (isConsumptionBuilding && isStorageBuilding) {
                        throw new RuntimeException("Building cannot be both storage and a consumer (causes issues with the loop) "+building.toString());
                    }

                    if (isConsumptionBuilding || isProductionBuilding) {
                        boolean needMoreResource = false;
                        boolean spaceAvailable = false;

                        if (isConsumptionBuilding) {
                            ConsumerComponent cc = building.getComponent(ConsumerComponent.class);
                            Map<String, Float> consumedResources = cc.consumption;
                            for (String resourceId : consumedResources.keySet()) {
                                Float amount = consumedResources.get(resourceId);
                                if (storedResourcesOutput.getOrDefault(resourceId, 0f) < amount) {
                                    needMoreResource = true;
                                    break;
                                }
                            }
                        }

                        if (isProductionBuilding) {
                            ProducerComponent pp = (ProducerComponent) building.getComponent(ProducerComponent.class);
                            Map<String, Float> producedResources = pp.production;
                            for (String resourceId : producedResources.keySet()) {
                                // System.out.println(Registry.getResourceRegistry().get(resourceId));
                                if (Registry.getResourceRegistry().get(resourceId).isPerTurnResource()) {
                                    spaceAvailable = true;
                                    break;
                                }
                                float totalResourcesAfter = storedResourcesOutput.getOrDefault(resourceId, 0f) + producedResources.getOrDefault(resourceId, 0f);
                                // If there is enough space for one "craft", allow the resource to be crafted
                                // TODO: if a building adds 3 and theres 9 with a capacity of 10, totalResourcesAfter is 12, but there is space for one craft
                                
                                //System.out.println(resourceId + ": " + storedResourcesOutput.getOrDefault(resourceId, 0f) + " / " + totalStorageCapacityOutput.getOrDefault(resourceId, 0f) + " (after: " + totalResourcesAfter + ")");
                                if (storedResourcesOutput.getOrDefault(resourceId, 0f) < totalStorageCapacityOutput.getOrDefault(resourceId, 0f)) {
                                    spaceAvailable = true;
                                    break;
                                }

                                /*if (totalResourcesAfter <= totalStorageCapacityOutput.getOrDefault(resourceId, 0f)) {
                                    spaceAvailable = true;
                                    break;
                                }*/
                            }
                        }

                        if (!((isConsumptionBuilding && needMoreResource) || (isProductionBuilding && !spaceAvailable)) || (isProductionBuilding && !isConsumptionBuilding)) {
                            if (isConsumptionBuilding) {
                                ConsumerComponent cc = building.getComponent(ConsumerComponent.class);
                                Map<String, Float> consumedResources = cc.consumption;
                                for (String resourceId : consumedResources.keySet()) {
                                    Float amount = consumedResources.get(resourceId);
                                    addResource.accept(resourceId, -amount);
                                }
                            }

                            if (isProductionBuilding) {
                                ProducerComponent pp = (ProducerComponent) building.getComponent(ProducerComponent.class);
                                Map<String, Float> producedResources = pp.production;
                                for (String resourceId : producedResources.keySet()) {
                                    Float amount = producedResources.get(resourceId);
                                    addResource.accept(resourceId, amount);
                                }
                            }

                            processed = true;
                        }
                    }

                    if (isStorageBuilding && !((isConsumptionBuilding || isProductionBuilding) && processed == false)) {
                        StorageComponent sc = building.getComponent(StorageComponent.class);
                        if (sc.maxStorage.size() == 0) {
                            buildingsProcessed[i] = true;
                            continue;
                        }
                        for (String resourceId : sc.maxStorage.keySet()) {
                            Float amount = sc.storage.getOrDefault(resourceId, 0f);
                            addResource.accept(resourceId, amount);
                            totalStorageCapacityOutput.put(resourceId, totalStorageCapacityOutput.getOrDefault(resourceId, 0f) + sc.maxStorage.getOrDefault(resourceId, 0f));
                            resourcesInStorageOutput.put(resourceId, resourcesInStorageOutput.getOrDefault(resourceId, 0f) + amount);
                        }
                        processed = true;
                    }

                    if (processed) {
                        buildingsProcessed[i] = true;
                        toBeActive.add(building);
                    }
                }

                // If buildingsProcessed matches finalBuildingsProcessed, then break
                if (Arrays.equals(buildingsProcessed, finalBuildingsProcessed)) {
                    System.err.println("Nothing changed, continuing.");
                    break;
                }

                System.arraycopy(buildingsProcessed, 0, finalBuildingsProcessed, 0, buildingsProcessed.length);
            } while (!everythingProcessed.call());
            // If everything is processed, continue
        } catch (Exception e) {
            e.printStackTrace();
        }

        return toBeActive;
    }

    /**
     * @param resourcesToStore
     * @return remaining resources that could not be stored (all storage full)
     */
    /*public Map<String, Float> distributeStorage(Map<String, Float> resourcesToStore) {
        // Clear all storage buildings
        Set<Entity> storageBuildings = engine.getEntities(Family.all(StorageComponent.class, OwnerComponent.class));
        // Distribute resources among storage buildings
        for (Entity building : storageBuildings) {
            StorageComponent storage = building.getComponent(StorageComponent.class);
            for (String resourceID : storage.getMaxStorage().keySet()) {
                storage.consumeAmount(resourceID, storage.getAmountStored(resourceID));
            }
        }
        Map<String, Float> remainingResources = new HashMap<>();
        for (String resourceID : resourcesToStore.keySet()) {
            float toAdd = resourcesToStore.get(resourceID);
            for (Entity building : storageBuildings) {
                StorageComponent storage = building.getComponent(StorageComponent.class);
                float remainingAfterAdd = storage.addAmount(resourceID, toAdd); 
                toAdd = remainingAfterAdd;
                if (toAdd == 0) break;
            }
            if (toAdd > 0) {
                remainingResources.put(resourceID, toAdd);
            }
        }
        // Update all storage buildings
        for (Entity building : storageBuildings) {
            engine.changedEntity(building);
        }
        return remainingResources;
    }*/

    public boolean tryUseResources(UUID playerUUID, Map<String, Float> resourcesToUse) {
        Map<String, Float> storedResources = new HashMap<>(this.cachedStoredResources.get(playerUUID));
        // Check if enough resources are available
        for (String resourceId : resourcesToUse.keySet()) {
            float amountToUse = resourcesToUse.get(resourceId);
            float amountInStorage = this.cachedStoredResources.get(playerUUID).getOrDefault(resourceId, 0f);
            if (amountInStorage < amountToUse) {
                return false;
            }
        }

        // If so, remove them from their storages
        for (String resource : resourcesToUse.keySet()) {
            Set<Entity> storageBuildings = engine.getEntities(Family.all(StorageComponent.class, OwnerComponent.class));
            float toUse = resourcesToUse.get(resource);
            System.out.println(toUse);
            for (Entity building : storageBuildings) {
                OwnerComponent owner = building.getComponent(OwnerComponent.class);
                if (owner == null || owner.playerID == null || !owner.playerID.equals(playerUUID.toString())) {
                    continue; // Not owned by this player
                }
                StorageComponent storage = building.getComponent(StorageComponent.class);
                float amountInThisStorage = storage.getAmountStored(resource);
                if (amountInThisStorage >= toUse) {
                    storage.consumeAmount(resource, toUse);
                    toUse = 0;
                } else {
                    storage.consumeAmount(resource, amountInThisStorage);
                    toUse -= amountInThisStorage;
                }
                engine.changedEntity(building);
                if (toUse == 0) break;
            }
        }

        calculateTurn(true);
        return true;
    }

    public Map<String, Float> getDisplayResourcePerSecond(UUID playerUUID) {
        return cachedGenerationPerSecond.get(playerUUID);
    }
    public Map<String, Float> getCachedStoredResources(UUID playerUUID) {
        return cachedStoredResources.get(playerUUID);
    }

    public List<Entity> getActiveEntities(UUID playerUUID) {
        return activeEntities.get(playerUUID);
    }

    public static Image getImageDisplay(Resource resource) {
        Image image = new Image(Sprites.getSprite(resource.getIcon()));
        return image;
    }
    public static Image getImageDisplay(String resourceId) {
        return getImageDisplay(Registry.getResourceRegistry().get(resourceId));
    }

    private UUID getLocalPlayerUUID() {
        if (playerScreen != null) {
            return playerScreen.getPlayerUUID();
        }
        return null;
    }

    @Override
    public void onTurn() {
        calculateTurn(false);
    }
    @Override
    public int getTurnPriority() {
        return 1;
    }

    

}
