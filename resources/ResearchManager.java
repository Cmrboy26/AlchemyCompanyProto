package net.cmr.alchemycompany;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResearchManager {
    
    public enum Technology {

        MINING(5, 1),
        SCIENCE(5, 1),
        MAGIC(25, 2),
        MILITARY(25, 2),
        FACTORY(25, 2),
        RESEARCH_LAB(45, 2),
        MAGICAL_WEAPONRY_I(45, 3),
        CHEMISTRY_I(35, 3),
        STORAGE(80, 3),
        BASIC_ALCHEMY(150, 4)
        ;

        public final float cost;
        public final int row;
        Technology(float cost, int row) {
            this.cost = cost;
            this.row = row;
        }

    }

    public static Map<Technology, Technology[]> prerequisitesMap;

    static {
        prerequisitesMap = new HashMap<>();

        prerequisitesMap.put(Technology.MINING, new Technology[]{});
        prerequisitesMap.put(Technology.SCIENCE, new Technology[]{});
        prerequisitesMap.put(Technology.MAGIC, new Technology[]{Technology.MINING});
        prerequisitesMap.put(Technology.MILITARY, new Technology[]{Technology.MINING});
        prerequisitesMap.put(Technology.FACTORY, new Technology[]{Technology.MINING});
        prerequisitesMap.put(Technology.RESEARCH_LAB, new Technology[]{Technology.SCIENCE});
        prerequisitesMap.put(Technology.MAGICAL_WEAPONRY_I, new Technology[]{Technology.MAGIC});
        prerequisitesMap.put(Technology.CHEMISTRY_I, new Technology[]{Technology.FACTORY});
        prerequisitesMap.put(Technology.STORAGE, new Technology[]{Technology.FACTORY, Technology.RESEARCH_LAB});
        prerequisitesMap.put(Technology.BASIC_ALCHEMY, new Technology[]{Technology.CHEMISTRY_I, Technology.STORAGE, Technology.MAGICAL_WEAPONRY_I});
    }

    Set<Technology> researchedTechnologies;
    Technology queuedResearch;
    float scienceRemaining;

    public ResearchManager() {
        this.researchedTechnologies = new HashSet<>();
    }

    public void queueTechnology(Technology tech) {
        queuedResearch = tech;
        scienceRemaining = tech.cost;
    }

    public void cancelTechnology() {
        queuedResearch = null;
        scienceRemaining = 0;
    }

    // returns if research complete
    public boolean addScience(float amount) {
        if (getQueuedTechnology() == null) {
            return false;
        }
        scienceRemaining -= amount;
        if (scienceRemaining <= 0) {
            researchedTechnologies.add(queuedResearch);
            cancelTechnology();
            return true;
        }
        return false;
    }

    public Technology getQueuedTechnology() {
        return queuedResearch;
    }

    public float getScienceRemaining() {
        return scienceRemaining;
    }

    public float getPercentCompletion() {
        return (queuedResearch.cost - scienceRemaining) / queuedResearch.cost;
    }

    public boolean isTechResearched(Technology tech) {
        return researchedTechnologies.contains(tech);
    }


}
