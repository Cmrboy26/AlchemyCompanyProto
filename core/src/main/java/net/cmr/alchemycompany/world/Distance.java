package net.cmr.alchemycompany.world;

public class Distance {
    
    public static double euclidian(double x, double y) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public static double manhattan(double x, double y) {
        return Math.abs(x) + Math.abs(y);
    }

    public static double chebyshev(double x, double y) {
        return Math.max(Math.abs(x), Math.abs(y));
    }

}
