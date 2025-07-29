package net.cmr.alchemycompany;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class IsometricHelper {
    
    private static Matrix3 isometricMatrix; 
    private static Matrix3 inverseMatrix;

    static {
        float[] values = new float[] 
            {1, -1, 0,
            0.5f, 0.5f, 0,
            0, 0, 1};
        isometricMatrix = new Matrix3(values);
        inverseMatrix = isometricMatrix.inv();
    }

    public static Vector3 standardToIsometric(Vector2 standard) {
        return standardToIsometric(new Vector3(standard, 0));
    }
    public static Vector3 standardToIsometric(float x, float y) {
        return standardToIsometric(new Vector3(x, y, 0));
    }
    public static Vector3 standardToIsometric(Vector3 standard) {
        return standard.mul(isometricMatrix);
    }

    public static Vector3 isometricToStandard(Vector2 standard) {
        return standardToIsometric(new Vector3(standard, 0));
    }
    public static Vector3 isometricToStandard(float x, float y) {
        return standardToIsometric(new Vector3(x, y, 0));
    }
    public static Vector3 isometricToStandard(Vector3 isometric) {
        return isometric.mul(inverseMatrix);
    }

}
