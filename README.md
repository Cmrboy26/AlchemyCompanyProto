# Alchemy Company Prototype
Prototype for a turn-based game where your goal is to alchemically synthesize gold and squash your competition through any economical, chemical, or magical means necessary.

### Installation
1: Download as ZIP<br>
2: Extract zip file
3: Ensure gradle is installed<br>
4: Run `gradlew lwjgl3:run`<br>

### Gameplay and Controls
Win Condition: Research "Basic Alchemy" and have 100 gold in storage.

- Scroll = Zoom
- Left Mouse + Space = Pan
- Left Mouse = Place, Select Troop
- Left Mouse + Left Shift = Place Multiple
- Right Mouse = Move Troop, Attack Troop / Building
- Left Control = Next Turn

## Project Summary
### Goal
The goal of this prototype was to create a minimal version of the game to:
- See if the core gameplay loop is enjoyable
- Test out implementations of core game systems
- Create a visual representation of my ideas
### MVP
- Build structures
- Produce and consume resources
- Queue research (tech tree)
- Troop movement and simple combat
- Win by synthesizing gold
- 6 biomes (water, plains, forest, mountains, swamp, crystal valley)
- 8 resources (gold, water, copper, iron, sulfur, sulfuric acid, crystal, witch eye)
- 8 buildings (headquarters, extractor, storage, factory, statue, research lab, archer tower, barracks)
- 3 Reaction Vessel Recipes
- 2 Troops (scout, soldier with 1 equipment slot)

### What Worked
- Turn-based resource generation is easier to implement and understand as a player
- Reaction vessels and equipment add complexity to the games combat
- Turn logic procedure progressed turns properly

###  What Didn't Work
- Separation between game logic and UI made new system implementation tedious
- A lack of UI helper methods made creating new UI difficult
- Resource "currency" in terms of units/second is not intuitive nor simple to implement
- Poor UX makes game unenjoyable and bland
    - Goals, Feedback, Phase Indicators, Effect Descriptions, Progress Feedback are important

### What I Learned
- No external pressure besides resource management makes gameplay stale
- Proper UI/UX makes gameplay more enjoyable
- Full game needs action systems for multiplayer and AI
- No placement restrictions or fog of war makes exploration pointless (and the game less engaging)
- ECS would help create reusable entity logic

### Next Prototype Goals
- Isometric tilemap
- Serializable game state
- Support AI and multiplayer-ready architecture
- Action queue
- Entity component system