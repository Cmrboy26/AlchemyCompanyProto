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
import java.util.stream.IntStream;

import com.badlogic.gdx.math.Vector2;

import net.cmr.alchemycompany.Building.AbstractStorageBuilding;
import net.cmr.alchemycompany.Building.ConsumptionBuilding;
import net.cmr.alchemycompany.Building.HeadquarterBuilding;
import net.cmr.alchemycompany.Building.ProductionBuilding;
import net.cmr.alchemycompany.Building.StorageBuilding;
import net.cmr.alchemycompany.Resources.Resource;

public class Player {

    String name;
    HeadquarterBuilding hq;
    Set<Building> buildings;
    Map<Resource, Float> calculatedResourcePerSecond;
    Map<Resource, Float> displayStoredResources;

    public Player(String name) {
        this.name = name;
        this.buildings = new HashSet<>();
        this.calculatedResourcePerSecond = new HashMap<>();
        this.displayStoredResources = new HashMap<>();
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
        } else {
            boolean successful = buildings.add(building);
            updateResourceConsumption();
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
            updateResourceConsumption();
            System.out.println("Remove:" + successful);
        }
    }

    public boolean consumeResource(Resource resource, float amount) {
        // TODO: Confirm that storages can remove enough, remove enough, return true if done, return false if cant
        throw new RuntimeException("Implement");
    }

    public void updateResourceConsumption() {

    }

    public void nextTurn() {
        // Produce and consume all resources
        List<Building> buildingsToProcess = new ArrayList<>(buildings);
        boolean[] finalBuildingsProcessed = new boolean[buildingsToProcess.size()];
        Arrays.fill(finalBuildingsProcessed, false);

        // Sort buildings here if needed

        buildingsToProcess.forEach(b -> {
            b.setIdle();
        });
        final HashMap<Resource, Float> storedResources = new HashMap<>();
        BiConsumer<Resource, Float> addResource = (r, f) -> {
            storedResources.put(r, storedResources.getOrDefault(r, 0f) + f);
        };
        Callable<Boolean> everythingProcessed = () -> {
            for (int i = 0; i < finalBuildingsProcessed.length; i++) {
                if (!finalBuildingsProcessed[i]) {
                    return false;
                }
            }
            return true;
        };

        // TODO: ISSUE: if there is no space for an item being transformed into something else (like sulfuric acid), the excess sulfur or water will not be stored
        // For things like this, we could store the amount of storage space available for any resource, and if the resource in storedResources is equal to the max,
        // then any building that produces that item should not produce anything until more gets consumed (or nothing will happen and it wont produce ever)
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

                    if (isProductionBuilding && !isConsumptionBuilding) {
                        ProductionBuilding pBuilding = (ProductionBuilding) building;
                        Map<Resource, Float> producedResources = pBuilding.getProductionPerTurn();
                        for (Resource resource : producedResources.keySet()) {
                            Float amount = producedResources.get(resource);
                            addResource.accept(resource, amount);
                        }
                        buildingsProcessed[i] = true;
                    }

                    if (isConsumptionBuilding) {
                        ConsumptionBuilding cBuilding = (ConsumptionBuilding) building;
                        Map<Resource, Float> consumedResources = cBuilding.getConsumptionPerTurn();

                        boolean needMoreResource = false;
                        for (Resource resource : consumedResources.keySet()) {
                            Float amount = consumedResources.get(resource);
                            if (storedResources.getOrDefault(resource, 0f) < amount) {
                                needMoreResource = true;
                                break;
                            }
                        }

                        if (!needMoreResource) {
                            for (Resource resource : consumedResources.keySet()) {
                                Float amount = consumedResources.get(resource);
                                addResource.accept(resource, -amount);
                            }

                            if (isProductionBuilding) {
                                ProductionBuilding pBuilding = (ProductionBuilding) building;
                                Map<Resource, Float> producedResources = pBuilding.getProductionPerTurn();
                                for (Resource resource : producedResources.keySet()) {
                                    Float amount = producedResources.get(resource);
                                    addResource.accept(resource, amount);
                                }
                            }
                            buildingsProcessed[i] = true;
                        }
                    }

                    if (isStorageBuilding) {
                        AbstractStorageBuilding sBuilding = (AbstractStorageBuilding) building;
                        for (Resource resource : sBuilding.getAllowedStoreResources()) {
                            Float amount = sBuilding.getAmountStored(resource);
                            sBuilding.consumeAmount(resource, amount);
                            addResource.accept(resource, amount);
                        }
                        buildingsProcessed[i] = true;
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

        System.out.println("Resources after turn:");
        for (Map.Entry<Resource, Float> entry : storedResources.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        final HashMap<Resource, Float> displayStoredResources = new HashMap<>();

        // Fill storages with remaining resources
        for (Building building : buildings) {
            if (building instanceof AbstractStorageBuilding) {
                AbstractStorageBuilding storage = (AbstractStorageBuilding) building;
                for (Resource resource : storage.getAllowedStoreResources()) {
                    float toAdd = storedResources.getOrDefault(resource, 0f);
                    float remainingAfterAdd = storage.addAmount(resource, toAdd);
                    storedResources.put(resource, remainingAfterAdd);
                    displayStoredResources.put(resource, displayStoredResources.getOrDefault(resource, 0f) + storage.getAmountStored(resource));
                }
            }
        }
        this.displayStoredResources = displayStoredResources;

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
