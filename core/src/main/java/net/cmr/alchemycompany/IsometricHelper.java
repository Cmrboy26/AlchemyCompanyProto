package net.cmr.alchemycompany;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import net.cmr.alchemycompany.system.RenderSystem;
import net.cmr.alchemycompany.world.TilePoint;
import net.cmr.alchemycompany.world.World;

public class IsometricHelper {
    
    private static Matrix3 standardToIsometric; 
    private static Matrix3 isometricToStandard;

    static {
        float[] values = new float[] 
            {1, -1, 0,
            0.5f, 0.5f, 0,
            0, 0, 1};
        isometricToStandard = new Matrix3(values);
        standardToIsometric = new Matrix3(values).inv();
    }

    public static Vector3 project(Vector2 standard) {
        return project(new Vector3(standard, 0));
    }
    public static Vector3 project(float x, float y) {
        return project(new Vector3(x, y, 0));
    }
    /**
     * Standard Coordinates -> Isometric Coordinates
     */
    public static Vector3 project(Vector3 standard) {
        return standard.cpy().mul(standardToIsometric);
    }

    public static Vector3 unproject(Vector2 isometric) {
        return unproject(new Vector3(isometric, 0));
    }
    public static Vector3 unproject(float x, float y) {
        return unproject(new Vector3(x, y, 0));
    }
    /**
     * Isometric Coordinates -> Standard Coordinates
     */
    public static Vector3 unproject(Vector3 isometric) {
        return isometric.cpy().mul(isometricToStandard);
    }

    public static TilePoint worldToIsometricTile(Vector3 worldCoords, World world) {
        return worldToIsometricTile(new Vector2(worldCoords.x, worldCoords.y), world);
    }
    public static TilePoint worldToIsometricTile(Vector2 worldCoords, World world) {
        float isoX = worldCoords.x / RenderSystem.TILE_SIZE;
        float isoY = worldCoords.y / (RenderSystem.TILE_SIZE / 4f);
        Vector3 tileCoords = IsometricHelper.unproject(isoX, isoY);
        int tileX = (int) Math.floor(tileCoords.x);
        int tileY = (int) Math.floor(tileCoords.y);
        if (tileX >= 0 && tileX < world.width && tileY >= 0 && tileY < world.height) {
            return new TilePoint(tileX, tileY);
        }
        return null;
    }

}
