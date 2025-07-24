package net.cmr.alchemycompany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.badlogic.gdx.math.Vector2;

import net.cmr.alchemycompany.Building.AbstractStorageBuilding;
import net.cmr.alchemycompany.Building.ConsumptionBuilding;
import net.cmr.alchemycompany.Building.HeadquarterBuilding;
import net.cmr.alchemycompany.Building.ProductionBuilding;
import net.cmr.alchemycompany.Resources.Resource;

public class Player {

    String name;
    HeadquarterBuilding hq;
    Set<Building> buildings;
    Map<Resource, Float> calculatedResourcePerSecond;
    Map<Resource, Float> currentStoredResources;
    Map<Resource, Float> calculatedNetStoredResourceNextTurn;
    Map<Pair<AbstractStorageBuilding, Resource>, Float> calculatedAmountToRemoveFromStorage = new HashMap<>();

    public Player(String name) {
        this.name = name;
        this.buildings = new HashSet<>();
        this.calculatedResourcePerSecond = new HashMap<>();
        this.currentStoredResources = new HashMap<>();
        this.calculatedNetStoredResourceNextTurn = new HashMap<>();
        this.calculatedAmountToRemoveFromStorage = new HashMap<>();
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
            System.out.println("YOU LOSE");
        } else {
            boolean successful = buildings.remove(building);
            updateResourceConsumption();
            System.out.println("Remove:" + successful);
        }
    }

    public void updateResourceConsumption() {
        final Map<Resource, Float> rps = new HashMap<>();
        final Map<Resource, Float> initialStoredResource = new HashMap<>();
        final Map<Resource, Float> calculatedStoredResource = new HashMap<>();
        final Map<Pair<AbstractStorageBuilding, Resource>, Float> amountToRemoveFromStorage = new HashMap<>();

        final List<AbstractStorageBuilding> storageList = buildings.stream().filter(t -> {
            return (t instanceof AbstractStorageBuilding);
        }).map(t -> { return (AbstractStorageBuilding) t; }).collect(Collectors.toList());
        final List<Building> pureProduction = buildings.stream().filter(t -> {
            return (t instanceof ProductionBuilding) && !(t instanceof ConsumptionBuilding);
        }).collect(Collectors.toList());
        final List<Building> transformBuilding = buildings.stream().filter(t -> {
            return (t instanceof ProductionBuilding) && (t instanceof ConsumptionBuilding);
        }).collect(Collectors.toList());
        final List<Building> consumptionBuilding = buildings.stream().filter(t -> {
            return (t instanceof ConsumptionBuilding) && !(t instanceof ProductionBuilding);
        }).collect(Collectors.toList());

        // Sum up storage
        storageList.forEach(storage -> {
            if (storage.getAllowedStoreResources() == null) return;
            for (Resource resource : storage.getAllowedStoreResources()) {
                initialStoredResource.put(resource, initialStoredResource.getOrDefault(resource, 0f) + storage.getAmountStored(resource));
                calculatedStoredResource.put(resource, calculatedStoredResource.getOrDefault(resource, 0f) + storage.getAmountStored(resource));
            }
        });

        // Process production
        pureProduction.forEach(b -> {
            ProductionBuilding production = (ProductionBuilding) b;
            production.getProductionPerTurn().forEach((r, f) -> {
                if (b.canFunction()) {
                    rps.put(r, rps.getOrDefault(r, 0f) + f);
                }
            });
        });

        Runnable processTransform = () -> {
            // Try and take things from all factories
            // Store if they have contributed to the rps as a map of booleans
            // If the last store of booleans is the same as the most recent calculation, go to next step
            // After this, make buildings that have not contributed idle

            List<Building> bList = new ArrayList<>();
            List<Boolean> contributed = new ArrayList<>();
            // initialize lists
            bList.addAll(transformBuilding);
            bList.addAll(consumptionBuilding);
            // TODO: Sort BList for priorities

            if (bList.size() == 0) {
                return;
            }
            for (int i = 0; i < bList.size(); i++) { bList.get(i).setActive(); }
            List<Boolean> calculatedContributedList = new ArrayList<>();

            do {
                System.out.println("Run");

                contributed = new ArrayList<>(calculatedContributedList);
                calculatedContributedList = new ArrayList<>();

                for (int i = 0; i < bList.size(); i++) {
                    Building building = bList.get(i);
                    // If the building has already contributed OR cannot function (destroyed), skip them
                    if (contributed.size() != 0 && (contributed.get(i) == true || !building.canFunction())) {
                        calculatedContributedList.add(true);
                        continue;
                    }
                    ConsumptionBuilding cBuilding = (ConsumptionBuilding) bList.get(i);
                    Map<Resource, Float> consumptionPerTurn = cBuilding.getConsumptionPerTurn();
                    boolean insufficientResourcesToContinue = false;
                    for (Entry<Resource, Float> entry : consumptionPerTurn.entrySet()) {
                        Resource r = entry.getKey();
                        Float f = entry.getValue();

                        float remainingRPS = rps.getOrDefault(r, 0f) - f;
                        if (remainingRPS < 0) {
                            // Not enough with just RPS, calculate with storage
                            float neededStorage = -remainingRPS;
                            if (calculatedStoredResource.getOrDefault(r, 0f) >= neededStorage) {
                                // Enough including storage
                                insufficientResourcesToContinue |= false;
                                System.out.println("Enough including storage");
                            } else {
                                // Not enough including storage
                                insufficientResourcesToContinue = true;
                                System.out.println("Not enough including storage");
                            }
                        } else {
                            // Enough with just RPS
                            insufficientResourcesToContinue |= false;
                            System.out.println("Enough with RPS");
                        }
                        //insufficientResourcesToContinue |= (rps.getOrDefault(r, 0f) - f < 0);
                        System.out.println(r + ", "+ insufficientResourcesToContinue);
                    };
                    if (insufficientResourcesToContinue) {
                        // Not enough resources right now, wait for later.
                        calculatedContributedList.add(false);
                        continue;
                    } else {
                        // Enough resources. Consume and produce
                        calculatedContributedList.add(true);
                        consumptionPerTurn.forEach((r, f) -> {
                            float putValue = rps.getOrDefault(r, 0f) - f;
                            if (putValue < 0) {
                                // Since this is true, there must be enough in storage (otherwise there would be insufficient resources)
                                float neededStorage = -putValue;
                                // Take the amount of resource out of storage from any building
                                for (int j = 0; j < storageList.size(); j++) {
                                    AbstractStorageBuilding storage = storageList.get(j);
                                    for (Resource storageResource : storage.getAllowedStoreResources()) {
                                        if (storageResource == r) {
                                            float calculatedAmountInStorage = storage.getAmountStored(storageResource) - amountToRemoveFromStorage.getOrDefault(storage, 0f);
                                            System.out.println(r.name() + " amount in storage "+calculatedAmountInStorage + " ("+storage.getAmountStored(storageResource) + " - " + amountToRemoveFromStorage.getOrDefault(storage, 0f) + "), need " + neededStorage);
                                            if (calculatedAmountInStorage >= neededStorage) {
                                                // Remove from storage in calculation
                                                amountToRemoveFromStorage.put(new Pair<>(storage, storageResource), amountToRemoveFromStorage.getOrDefault(storage, 0f) + neededStorage);
                                                calculatedStoredResource.put(r, calculatedStoredResource.getOrDefault(r, 0f) - neededStorage);
                                                neededStorage = 0;
                                                // Everything has been taken from needed storages, so continue
                                                break;
                                            } else {
                                                // Not enough to fully cover the amount needed, but add as much as possible.
                                                neededStorage -= calculatedAmountInStorage;
                                                amountToRemoveFromStorage.put(new Pair<>(storage, storageResource), calculatedAmountInStorage);
                                                calculatedStoredResource.put(r, calculatedStoredResource.getOrDefault(r, 0f) - calculatedAmountInStorage);
                                            }
                                        }
                                    }
                                }

                                // If there WASN'T enough in storage (neededStorage > 0), then we messed up something with our insufficientResource calculation
                                if (neededStorage > 0) {
                                    throw new RuntimeException("Insufficient supply of resource in storage calculation for resource " + r + " in building " + building.toString());
                                }

                                putValue = 0;
                            }
                            rps.put(r, putValue);
                        });
                        if (building instanceof ProductionBuilding) {
                            ProductionBuilding pBuilding = (ProductionBuilding) bList.get(i);
                            Map<Resource, Float> productionPerTurn = pBuilding.getProductionPerTurn();
                            productionPerTurn.forEach((r, f) -> {
                                rps.put(r, rps.getOrDefault(r, 0f) + f);
                            });
                        }
                        continue;
                    }
                }

                // If everything has contributed, continue
                if (calculatedContributedList.stream().allMatch(Boolean::booleanValue)) {
                    contributed = calculatedContributedList;
                    break;
                }
            } while (!contributed.equals(calculatedContributedList));

            for (int i = 0; i < bList.size(); i++) {
                if (contributed.get(i)) {
                    bList.get(i).setActive();
                } else {
                    bList.get(i).setIdle();
                }
            }
            System.out.println("Process transform completed");

        };

        processTransform.run();


        /*List<ProductionBuilding> productionBuildings = new ArrayList<>();
        buildings.forEach((Building b) -> {
            if (b instanceof ProductionBuilding) {
                productionBuildings.add((ProductionBuilding) b);
            }
        });
        for (final ProductionBuilding building : productionBuildings) {
            Map<Resource, Float> buildingRPS = building.getProductionPerTurn();
            buildingRPS.forEach((Resource r, Float f) -> {
                if (((Building) building).canFunction()) {
                    rps.put(r, rps.getOrDefault(r, 0f) + f);
                }
            });
        }
        List<ConsumptionBuilding> consumptionBuildings = new ArrayList<>();
        buildings.forEach((Building b) -> {
            if (b instanceof ConsumptionBuilding) {
                consumptionBuildings.add((ConsumptionBuilding) b);
            }
        });
        for (ConsumptionBuilding building : consumptionBuildings) {
            Map<Resource, Float> buildingRPS = building.getConsumptionPerTurn();
            buildingRPS.forEach((Resource r, Float f) -> {
                float putValue = rps.getOrDefault(r, 0f) - f;
                if (putValue < 0) {
                    ((Building) building).setIdle();
                    System.out.println("NOT ENOUGH " + r.name() + " TO SUPPLY " + building.getClass().getSimpleName() + " (would be "+putValue+" rps after)");
                    return;
                } else {
                    ((Building) building).setActive();
                    if (((Building) building).canFunction()) {
                        rps.put(r, putValue);
                    }
                }
            });
        }*/

        rps.forEach((r, f) -> {
            System.out.println(r.name() + ", " + f);
        });
        calculatedResourcePerSecond = rps;
        currentStoredResources = initialStoredResource;
        calculatedAmountToRemoveFromStorage = amountToRemoveFromStorage;

        // Convert amountToRemoveFromStorage to Map<Resource, Float>
        Map<Resource, Float> netStoredResourceNextTurn = new HashMap<>();
        for (Map.Entry<Pair<AbstractStorageBuilding, Resource>, Float> entry : amountToRemoveFromStorage.entrySet()) {
            //for (Resource resource : entry.getKey().get.getAllowedStoreResources()) {
            AbstractStorageBuilding building = entry.getKey().getFirst();
            Resource resource = entry.getKey().getSecond();
            netStoredResourceNextTurn.put(resource, netStoredResourceNextTurn.getOrDefault(resource, 0f) - entry.getValue());
            System.out.println("WOAH: "+resource+" - "+netStoredResourceNextTurn.get(resource));
            //}
        }
        calculatedNetStoredResourceNextTurn = netStoredResourceNextTurn;
    }

    public void nextTurn() {
        // Update resource consumption JUUUST in case
        updateResourceConsumption();
        // Consume resources from storage
        calculatedAmountToRemoveFromStorage.forEach((p, f) -> {
            AbstractStorageBuilding b = p.getFirst();
            Resource r = p.getSecond();
            System.out.println(b.getX() +", "+b.getY() + ", " + f);
            b.consumeAmount(r, f);
        });
        // Produce excess resources
        calculatedResourcePerSecond.forEach((r, f) -> {
            float remaining = f;
            System.err.println("REMAINING: "+r+", "+f);
            for (AbstractStorageBuilding storage : buildings.stream()
                    .filter(b -> b instanceof AbstractStorageBuilding)
                    .map(b -> (AbstractStorageBuilding) b)
                    .filter(b -> Arrays.asList(b.getAllowedStoreResources()).contains(r))
                    .collect(Collectors.toList())) {
                if (remaining == 0) break;
                remaining = storage.addAmount(r, remaining);
                System.out.println("remaining after storage put: "+remaining);
            }
        });
        updateResourceConsumption();
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
