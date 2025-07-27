package net.cmr.alchemycompany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import net.cmr.alchemycompany.Building.AbstractStorageBuilding;
import net.cmr.alchemycompany.Building.ConsumptionBuilding;
import net.cmr.alchemycompany.Building.HeadquarterBuilding;
import net.cmr.alchemycompany.Building.ProductionBuilding;
import net.cmr.alchemycompany.Resources.Resource;
import net.cmr.alchemycompany.troops.Troop;

public class Player {

    String name;
    HeadquarterBuilding hq;
    Set<Building> buildings;
    Map<Resource, Float> calculatedResourcePerSecond;
    Map<Resource, Float> displayStoredResources;
    ResearchManager researchManager;
    public Troop selectedTroop;

    public static final float MAX_SCIENCE_PER_SECOND = 100000;

    public Player(String name) {
        this.name = name;
        this.buildings = new HashSet<>();
        this.calculatedResourcePerSecond = new HashMap<>();
        this.displayStoredResources = new HashMap<>();
        this.researchManager = new ResearchManager();
    }

    public void setHQ(HeadquarterBuilding building) {
        this.hq = building;
    }

    public HeadquarterBuilding getHQ() {
        return hq;
    }

    public Vector2 getHQPosition() {
        return new Vector2(hq.getX(), hq.getY());
    }

    public void addBuilding(Building building) {
        if (building instanceof HeadquarterBuilding) {
            setHQ((HeadquarterBuilding) building);
            buildings.add(building);
            building.built = true;
        } else {
            boolean successful = buildings.add(building);
            updateResourceDisplay();
            System.out.println("Add: " + successful);
        }
    }

    public void removeBuilding(Building building) {
        if (building instanceof HeadquarterBuilding) {
            // LOSE
            hq = null;
            buildings.remove(building);
            System.out.println("YOU LOSE");
        } else {
            boolean successful = buildings.remove(building);
            updateResourceDisplay();
            System.out.println("Remove:" + successful);
        }
    }

    public int getCountOf(Class<? extends Building> buildingClass) {
        return (int) buildings.stream()
                .filter(b -> b.getClass().equals(buildingClass))
                .count();
    }

    public boolean consumeResource(Resource resource, float amount) {
        // TODO: Confirm that storages can remove enough, remove enough, return true if done, return false if cant
        int count = 0;
        for (Building building : buildings) {
            if (building instanceof AbstractStorageBuilding) {
                AbstractStorageBuilding storage = (AbstractStorageBuilding) building;
                if (storage.getAllowedStoreResources() == null || storage.getAllowedStoreResources().length == 0) {
                    continue; // No resources to store, skip
                }
                count += storage.getAmountStored(resource);
            }
        }

        if (count >= amount) {
            float remainingToConsume = amount;
            for (Building building : buildings) {
                if (building instanceof AbstractStorageBuilding) {
                    AbstractStorageBuilding storage = (AbstractStorageBuilding) building;
                    if (storage.getAllowedStoreResources() == null || storage.getAllowedStoreResources().length == 0) {
                        continue; // No resources to store, skip
                    }
                    float available = storage.getAmountStored(resource);
                    float amountToConsume = Math.min(remainingToConsume, available);
                    if (amountToConsume > 0) {
                        storage.consumeAmount(resource, amountToConsume);
                        remainingToConsume -= amountToConsume;
                        if (remainingToConsume <= 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void updateResourceDisplay() {
        Map<Resource, Float> generationPerSecond = new HashMap<>();
        Map<Resource, Float> trueResourcesInStorage = new HashMap<>();
        Map<Resource, Float> calculatedStoredResources = new HashMap<>();
        Map<Resource, Float> calculatedTotalStorageCapacity = new HashMap<>();

        List<Building> toBeActive = calculateBuildingOutput(true, trueResourcesInStorage, calculatedStoredResources, calculatedTotalStorageCapacity);

        for (Resource resource : Resource.values()) {
            float inStorage = trueResourcesInStorage.getOrDefault(resource, 0f);
            float afterTurn = calculatedStoredResources.getOrDefault(resource, 0f);
            if (resource != Resource.SCIENCE) {
                afterTurn = Math.min(afterTurn, calculatedTotalStorageCapacity.getOrDefault(resource, 0f));
            }
            float rps = afterTurn - inStorage;
            generationPerSecond.put(resource, rps);
        }
        for (Building building : toBeActive) {
            building.setActive();
        }
        System.out.println("calculated update resource display");

        this.displayStoredResources = trueResourcesInStorage;
        this.calculatedResourcePerSecond = generationPerSecond;
    }

    public void nextTurn(GameScreen screen) {
        // Consume and Produce
        Map<Resource, Float> storedResources = new HashMap<>();
        Map<Resource, Float> totalStorage = new HashMap<>();

        List<Building> toBeActive = calculateBuildingOutput(true, new HashMap<>(), storedResources, totalStorage);

        for (Building building : toBeActive) {
            building.setActive();
        }

        // Progress research
        float sciencePointsGained = storedResources.getOrDefault(Resource.SCIENCE, 0f);
        System.out.println("Gained +"+sciencePointsGained+" science this turn");
        boolean scienceResearched = researchManager.addScience(sciencePointsGained);
        if (scienceResearched) {
            screen.researchButton.clearActions();
            screen.researchButton.addAction(Actions.sequence(Actions.run(() -> {
                screen.researchButton.setText("RESEARCH\nCOMPLETE");
            }), Actions.delay(2),Actions.run(() -> {
                screen.researchButton.setText("Research");
            })));
        }
        storedResources.remove(Resource.SCIENCE);

        // Fill storages with remaining resources
        for (Building building : buildings) {
            if (building instanceof AbstractStorageBuilding) {
                AbstractStorageBuilding storage = (AbstractStorageBuilding) building;
                if (storage.getAllowedStoreResources() == null || storage.getAllowedStoreResources().length == 0) {
                    continue; // No resources to store, skip
                }
                for (Resource resource : storage.getAllowedStoreResources()) {
                    storage.consumeAmount(resource, storage.getAmountStored(resource));
                    float toAdd = storedResources.getOrDefault(resource, 0f);
                    float remainingAfterAdd = storage.addAmount(resource, toAdd); 
                    storedResources.put(resource, remainingAfterAdd);
                    //displayStoredResources.put(resource, displayStoredResources.getOrDefault(resource, 0f) + storage.getAmountStored(resource));
                }
            }
        }
        
        updateResourceDisplay();

        // Move enemy troops (if applicable)
        // Update buildings
        for (Building building : buildings) {
            building.onTurn();
        }
    }

    /**
     * @param resourcesInStorageOutput used for display in calculation
     * @param storedResourcesOutput amount of resources to be stored at the end of a turn
     * @param totalStorageCapacityOutput total amount of storage capacity (max holdable resources)
     * @return list of buildings that should be converted to active instead of idle
     */
    public List<Building> calculateBuildingOutput(boolean setBuildingsIdle, final Map<Resource, Float> resourcesInStorageOutput, final Map<Resource, Float> storedResourcesOutput, final Map<Resource, Float> totalStorageCapacityOutput) {
        // Produce and consume all resources
        List<Building> buildingsToProcess = new ArrayList<>(buildings);
        List<Building> toBeActive = new ArrayList<>();
        boolean[] finalBuildingsProcessed = new boolean[buildingsToProcess.size()];
        Arrays.fill(finalBuildingsProcessed, false);

        // Sort buildings here if needed

        if (setBuildingsIdle) {
            buildingsToProcess.forEach(b -> {
                b.setIdle();
            });
        }

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
                    Building building = buildingsToProcess.get(i);

                    if (buildingsProcessed[i]) {
                        // Already processed, continue
                        continue;
                    }

                    if (!building.canFunction()) {
                        buildingsProcessed[i] = true;
                        continue;
                    }

                    boolean isProductionBuilding = building instanceof ProductionBuilding;
                    boolean isConsumptionBuilding = building instanceof ConsumptionBuilding;
                    boolean isStorageBuilding = building instanceof AbstractStorageBuilding;
                    boolean processed = false;

                    if (isConsumptionBuilding && isStorageBuilding) {
                        throw new RuntimeException("Building cannot be both storage and a consumer (causes issues with the loop) "+building.toString());
                    }

                    if (isConsumptionBuilding || isProductionBuilding) {
                        boolean needMoreResource = false;
                        boolean spaceAvailable = false;

                        if (isConsumptionBuilding) {
                            ConsumptionBuilding cBuilding = (ConsumptionBuilding) building;
                            Map<Resource, Float> consumedResources = cBuilding.getConsumptionPerTurn();
                            for (Resource resource : consumedResources.keySet()) {
                                Float amount = consumedResources.get(resource);
                                if (storedResourcesOutput.getOrDefault(resource, 0f) < amount) {
                                    needMoreResource = true;
                                    break;
                                }
                            }
                        }

                        if (isProductionBuilding) {
                            ProductionBuilding pBuilding = (ProductionBuilding) building;
                            Map<Resource, Float> producedResources = pBuilding.getProductionPerTurn();
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
                                ConsumptionBuilding cBuilding = (ConsumptionBuilding) building;
                                Map<Resource, Float> consumedResources = cBuilding.getConsumptionPerTurn();
                                for (Resource resource : consumedResources.keySet()) {
                                    Float amount = consumedResources.get(resource);
                                    addResource.accept(resource, -amount);
                                }
                            }

                            if (isProductionBuilding) {
                                ProductionBuilding pBuilding = (ProductionBuilding) building;
                                Map<Resource, Float> producedResources = pBuilding.getProductionPerTurn();
                                for (Resource resource : producedResources.keySet()) {
                                    Float amount = producedResources.get(resource);
                                    addResource.accept(resource, amount);
                                }
                            }

                            processed = true;
                        }
                    }

                    if (isStorageBuilding && !((isConsumptionBuilding || isProductionBuilding) && processed == false)) {
                        AbstractStorageBuilding sBuilding = (AbstractStorageBuilding) building;
                        if (sBuilding.getAllowedStoreResources() == null) {
                            buildingsProcessed[i] = true;
                            continue;
                        }
                        for (Resource resource : sBuilding.getAllowedStoreResources()) {
                            Float amount = sBuilding.getAmountStored(resource);
                            addResource.accept(resource, amount);
                            totalStorageCapacityOutput.put(resource, totalStorageCapacityOutput.getOrDefault(resource, 0f) + sBuilding.getMaxAmount());
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

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Player) && ((Player) obj).name.equals(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
