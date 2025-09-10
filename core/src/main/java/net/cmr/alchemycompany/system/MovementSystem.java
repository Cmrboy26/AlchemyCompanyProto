package net.cmr.alchemycompany.system;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.cmr.alchemycompany.ACEngine;
import net.cmr.alchemycompany.GameManager;
import net.cmr.alchemycompany.ITurnSystem;
import net.cmr.alchemycompany.IUpdateSystem;
import net.cmr.alchemycompany.component.MovementComponent;
import net.cmr.alchemycompany.component.OwnerComponent;
import net.cmr.alchemycompany.component.PlacementComponent;
import net.cmr.alchemycompany.component.TilePositionComponent;
import net.cmr.alchemycompany.component.actions.IActionComponent;
import net.cmr.alchemycompany.component.actions.MovementActionComponent;
import net.cmr.alchemycompany.ecs.Engine;
import net.cmr.alchemycompany.ecs.Entity;
import net.cmr.alchemycompany.ecs.EntitySystem;
import net.cmr.alchemycompany.ecs.Family;
import net.cmr.alchemycompany.network.GameServer;
import net.cmr.alchemycompany.world.Tile;
import net.cmr.alchemycompany.world.TilePoint;
import net.cmr.alchemycompany.world.World;

public class MovementSystem extends EntitySystem implements IUpdateSystem, ITurnSystem {
    
    Family movementFamily;
    GameServer server;

    @Override
    public void addedToEngine(Engine engine) {
        movementFamily = Family.all(TilePositionComponent.class, MovementComponent.class, OwnerComponent.class);
    }

    @Override
    public void update(float delta) {
        IActionComponent.processActionEntities(engine, MovementActionComponent.class, (entity, pac, mac) -> {
            Entity movementEntity = engine.getEntity(mac.getEntityUUID());
            if (movementEntity == null) {
                return;
            }
            if (!movementFamily.matches(movementEntity)) {
                return;
            }
            OwnerComponent oc = movementEntity.getComponent(OwnerComponent.class);
            if (!pac.playerUUID.toString().equals(oc.playerID)) {
                return;
            }

            MovementPath movementPath = new MovementPath(movementEntity, engine.as(ACEngine.class));
            try {

                movementPath.calculate(mac.getTargetX(), mac.getTargetY());
                followMovementPath(pac.playerUUID, movementEntity, movementPath);
            } catch (Exception e) {
                // Player sent in an input that would not have calculated correctly.
                e.printStackTrace();
            }
        });
    }

    private void followMovementPath(UUID playerUUID, Entity entity, MovementPath path) {
        TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
        MovementComponent mc = entity.getComponent(MovementComponent.class);
        List<TilePoint> pathList = path.getMovementPath();
        World world = engine.as(ACEngine.class).getWorld();
        world.getTile(tpc.tileX, tpc.tileY).setUnitSlotID(null);
        for (int i = 1; i < pathList.size() && mc.movesRemaining > 0; i++) {
            tpc.tileX = pathList.get(i).getX();
            tpc.tileY = pathList.get(i).getY();
            // TODO: Increase movement cost for certain world features like mountains
            mc.movesRemaining--;
        }
        world.getTile(tpc.tileX, tpc.tileY).setUnitSlotID(entity.getID());
        engine.changedEntity(entity);
        GameManager.onPlacementChange(playerUUID, tpc.tileX, tpc.tileY, engine);
    }

    public static boolean isValidMovementTile(Entity entity, TilePoint tp, World world) {
        if (!world.isInWorld(tp)) { return false; }
        Tile tile = world.getTile(tp.getX(), tp.getY());
        if (!entity.hasComponent(PlacementComponent.class)) { return false; }
        if (tile == null) { return false; }
        if (!tile.canPlaceUnit()) { return false; }
        return entity.getComponent(PlacementComponent.class).getValidPlacement().contains(tile.getFeature());
    }

    @Override
    public int getTurnPriority() {
        return 3;
    }

    @Override
    public void onTurn() {
        for (Entity entity : engine.getEntities(movementFamily)) {
            entity.getComponent(MovementComponent.class).resetMovement();
            engine.changedEntity(entity);
        }
    }

    public static class MovementPath {

        List<TilePoint> movementPath;
        transient Entity entity;
        transient ACEngine engine;

        public MovementPath(Entity entity, ACEngine engine) {
            this.movementPath = new LinkedList<>();
            this.entity = entity;
            this.engine = engine;
        }

        private class AStarCell {

            public float g, h;
            public AStarCell(float g, float h) {
                this.g = g;
                this.h = h;
            }

            public float calculateF() {
                return g + h;
            }

        }

        /**
         * @return list of tile points calculated by the A* algorithm. The first element is the starting tile or current position.
         */
        public List<TilePoint> getMovementPath() {
            return movementPath;
        }

        /**
         * Uses A* pathfinding algorithm to determine how to travel to the specified tile
         * @param targetX target position X coordinate
         * @param targetY target position Y coordinate
         */
        public void calculate(int targetX, int targetY) throws IOException {
            MovementComponent mc = entity.getComponent(MovementComponent.class);
            TilePositionComponent tpc = entity.getComponent(TilePositionComponent.class);
            PlacementComponent pc = entity.getComponent(PlacementComponent.class);
            World world = engine.as(ACEngine.class).getWorld();

            // A* pathfinding algorithm
            final AStarCell defaultStarCell = new AStarCell(Integer.MAX_VALUE, Integer.MAX_VALUE); // ik it's a float, but i dont want any overflow

            TilePoint startTile = new TilePoint(tpc.tileX, tpc.tileY);
            TilePoint endTile = new TilePoint(targetX, targetY);
            Map<TilePoint, AStarCell> tileValues = new HashMap<>();
            Comparator<TilePoint> comparator = Comparator.comparingDouble((tilePoint) -> {
                return tileValues.getOrDefault(tilePoint, defaultStarCell).calculateF();
            });
            Queue<TilePoint> openSet = new PriorityQueue<TilePoint>(comparator);
            Set<TilePoint> closedSet = new HashSet<>();
            Map<TilePoint, TilePoint> cameFrom = new HashMap<>();
            
            openSet.add(startTile);
            tileValues.put(startTile, new AStarCell(0, 0));
            boolean targetReached = false;

            // If a player sends a target position that's the same as the end tile, just say it worked
            if (startTile.equals(endTile)) {
                targetReached = true;
            }
            if (!world.isInWorld(endTile)) {
                throw new IOException("End tile must be in world.");
            }
            boolean endIsTraversable = !pc.getValidPlacement().contains(world.getTile(endTile).getFeature());
            boolean endIsOccupied = !world.getTile(endTile).canPlaceUnit();
            if (endIsTraversable || endIsOccupied) {
                throw new IOException("End tile must be traversable.");
            }

            while (!openSet.isEmpty() && !targetReached) {
                // Prioritize lowest cost path (lowest f)
                TilePoint q = openSet.poll();
                AStarCell qCell = tileValues.get(q);
                int[] dx = {1, 0, -1, 0};
                int[] dy = {0, 1, 0, -1};
                for (int i = 0; i < 4; i++) {
                    TilePoint candidate = new TilePoint(q.getX() + dx[i], q.getY() + dy[i]);
                    if (closedSet.contains(candidate)) {
                        continue;
                    }
                    if (candidate.equals(endTile)) {
                        // WE FOUND IT!!
                        cameFrom.put(candidate, q);
                        targetReached = true;
                        break;
                    }
                    // Check if the TilePoint is traversable
                    if (!isValidMovementTile(entity, candidate, world)) {
                        continue;
                    }

                    float g = qCell.g + 1;
                    AStarCell existingSpot = tileValues.get(candidate);
                    if (existingSpot != null && g >= existingSpot.g) {
                        continue;
                    }

                    // If it is traversable, add it to the open set
                    openSet.remove(candidate);
                    openSet.add(candidate);
                    cameFrom.put(candidate, q);
                    // Calculate A* values
                    float h = Math.abs(targetX - candidate.getX()) + Math.abs(targetY - candidate.getY());
                    AStarCell candidateCell = new AStarCell(g, h);
                    tileValues.put(candidate, candidateCell);
                }
                if (targetReached) {
                    System.out.println("Target reached.");
                    break;
                }
                closedSet.add(q);
            }

            if (!targetReached) {
                throw new IOException("No path to target tile "+endTile.getX()+", "+endTile.getY());
            }

            TilePoint backtrackPoint = endTile;
            movementPath.clear();
            do {
                System.out.println(backtrackPoint);
                movementPath.add(backtrackPoint);
                backtrackPoint = cameFrom.get(backtrackPoint);
            } while (backtrackPoint != null);
            // First index is the start tile
            Collections.reverse(movementPath);
        }
    }

}
