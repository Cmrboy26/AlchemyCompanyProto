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
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.game.Resources.Resource;

public class ResourceSystem extends EntitySystem {

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
    }

    private class ResourcesOutcome {

    }

    public void calculateTurn() {
        Map<Resource, Float> generationPerSecond = new HashMap<>();
        Map<Resource, Float> trueResourcesInStorage = new HashMap<>();
        Map<Resource, Float> calculatedStoredResources = new HashMap<>();
        Map<Resource, Float> calculatedTotalStorageCapacity = new HashMap<>();

        calculateBuildingOutput(trueResourcesInStorage, calculatedStoredResources, calculatedTotalStorageCapacity);

        for (Resource resource : Resource.values()) {
            float inStorage = trueResourcesInStorage.getOrDefault(resource, 0f);
            float afterTurn = calculatedStoredResources.getOrDefault(resource, 0f);
            if (resource != Resource.SCIENCE) {
                afterTurn = Math.min(afterTurn, calculatedTotalStorageCapacity.getOrDefault(resource, 0f));
            }
            float rps = afterTurn - inStorage;
            generationPerSecond.put(resource, rps);
        }

        for (Resource resource : generationPerSecond.keySet()) {
            System.out.println(resource + ", "+generationPerSecond.get(resource));
        }
    }

    /**
     * @param resourcesInStorageOutput used for display in calculation
     * @param storedResourcesOutput amount of resources to be stored at the end of a turn
     * @param totalStorageCapacityOutput total amount of storage capacity (max holdable resources)
     * @return list of buildings that should be converted to active instead of idle
     */
    public List<Entity> calculateBuildingOutput(final Map<Resource, Float> resourcesInStorageOutput, final Map<Resource, Float> storedResourcesOutput, final Map<Resource, Float> totalStorageCapacityOutput) {
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
        BiConsumer<Resource, Float> addResource = (r, f) -> {
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
                            Map<Resource, Float> consumedResources = cc.consumption;
                            for (Resource resource : consumedResources.keySet()) {
                                Float amount = consumedResources.get(resource);
                                if (storedResourcesOutput.getOrDefault(resource, 0f) < amount) {
                                    needMoreResource = true;
                                    break;
                                }
                            }
                        }

                        if (isProductionBuilding) {
                            ProducerComponent pp = (ProducerComponent) building.getComponent(ProducerComponent.class);
                            Map<Resource, Float> producedResources = pp.production;
                            for (Resource resource : producedResources.keySet()) {
                                if (resource == Resource.SCIENCE) {
                                    spaceAvailable = true;
                                    break;
                                }
                                float totalResourcesAfter = storedResourcesOutput.getOrDefault(resource, 0f) + producedResources.getOrDefault(resource, 0f);
                                // If there is enough space for one "craft", allow the resource to be crafted
                                if (totalResourcesAfter <= totalStorageCapacityOutput.getOrDefault(resource, 0f)) {
                                    spaceAvailable = true;
                                    break;
                                }
                            }
                        }

                        if (!((isConsumptionBuilding && needMoreResource) || (isProductionBuilding && !spaceAvailable)) || (isProductionBuilding && !isConsumptionBuilding)) {
                            if (isConsumptionBuilding) {
                                ConsumerComponent cc = building.getComponent(ConsumerComponent.class);
                                Map<Resource, Float> consumedResources = cc.consumption;
                                for (Resource resource : consumedResources.keySet()) {
                                    Float amount = consumedResources.get(resource);
                                    addResource.accept(resource, -amount);
                                }
                            }

                            if (isProductionBuilding) {
                                ProducerComponent pp = (ProducerComponent) building.getComponent(ProducerComponent.class);
                                Map<Resource, Float> producedResources = pp.production;
                                for (Resource resource : producedResources.keySet()) {
                                    Float amount = producedResources.get(resource);
                                    addResource.accept(resource, amount);
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
                        for (Resource resource : sc.maxStorage.keySet()) {
                            Float amount = sc.storage.getOrDefault(resource, 0f);
                            addResource.accept(resource, amount);
                            totalStorageCapacityOutput.put(resource, totalStorageCapacityOutput.getOrDefault(resource, 0f) + sc.maxStorage.getOrDefault(resource, 0f));
                            resourcesInStorageOutput.put(resource, resourcesInStorageOutput.getOrDefault(resource, 0f) + amount);
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

}
