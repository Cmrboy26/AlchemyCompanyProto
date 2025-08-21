package net.cmr.alchemycompany.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import net.cmr.alchemycompany.component.ConsumerComponent;
import net.cmr.alchemycompany.component.ProducerComponent;
import net.cmr.alchemycompany.component.StorageComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.game.Registry;
import net.cmr.alchemycompany.game.Resource;
import net.cmr.alchemycompany.game.Registry.ResourceFilter;

public class ResourceSystem extends EntitySystem {

    private Map<String, Float> displayGenerationPerSecond = new HashMap<>();

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }

    private class ResourcesOutcome {

    }

    public void calculateTurn() {
        Map<String, Float> generationPerSecond = new HashMap<>();
        Map<String, Float> trueResourcesInStorage = new HashMap<>();
        Map<String, Float> calculatedStoredResources = new HashMap<>();
        Map<String, Float> calculatedTotalStorageCapacity = new HashMap<>();

        calculateBuildingOutput(trueResourcesInStorage, calculatedStoredResources, calculatedTotalStorageCapacity);

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

        System.out.println("Resource system calculating");
        generationPerSecond.keySet().stream()
            .sorted()
            .filter((rid) -> { return generationPerSecond.get(rid) != 0; })
            .forEach(resourceId ->
            System.out.print(resourceId + ", " + generationPerSecond.get(resourceId) + "\t")
            );
        System.out.println();
        displayGenerationPerSecond = new HashMap<>(generationPerSecond);
    }

    /**
     * @param resourcesInStorageOutput used for display in calculation
     * @param storedResourcesOutput amount of resources to be stored at the end of a turn
     * @param totalStorageCapacityOutput total amount of storage capacity (max holdable resources)
     * @return list of buildings that should be converted to active instead of idle
     */
    public List<Entity> calculateBuildingOutput(final Map<String, Float> resourcesInStorageOutput, final Map<String, Float> storedResourcesOutput, final Map<String, Float> totalStorageCapacityOutput) {
        // Produce and consume all resources
        List<Entity> buildingsToProcess = new ArrayList<>();
        // Use a set to avoid duplicates
        java.util.Set<Entity> uniqueEntities = new java.util.HashSet<>();
        uniqueEntities.addAll(engine.getComponentMapper(ProducerComponent.class));
        uniqueEntities.addAll(engine.getComponentMapper(ConsumerComponent.class));
        uniqueEntities.addAll(engine.getComponentMapper(StorageComponent.class));
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

                    boolean isProductionBuilding = building.hasComponent(ProducerComponent.class);
                    boolean isConsumptionBuilding = building.hasComponent(ConsumerComponent.class);
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
                                if (totalResourcesAfter <= totalStorageCapacityOutput.getOrDefault(resourceId, 0f)) {
                                    spaceAvailable = true;
                                    break;
                                }
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

    public Map<String, Float> getDisplayResourcePerSecond() {
        return displayGenerationPerSecond;
    }

}
