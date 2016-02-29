package org.matsim.contrib.util.distance;

import org.matsim.api.core.v01.*;


public class DistanceUtils
{
    public static double calculateDistance(BasicLocation<?> fromLocation,
            BasicLocation<?> toLocation)
    {
        return calculateDistance(fromLocation.getCoord(), toLocation.getCoord());
    }


    public static double calculateSquaredDistance(BasicLocation<?> fromLocation,
            BasicLocation<?> toLocation)
    {
        return calculateSquaredDistance(fromLocation.getCoord(), toLocation.getCoord());
    }


    public static double calculateDistance(Coord fromCoord, Coord toCoord)
    {
        return Math.sqrt(calculateSquaredDistance(fromCoord, toCoord));
    }


    public static double calculateSquaredDistance(Coord fromCoord, Coord toCoord)
    {
        double deltaX = toCoord.getX() - fromCoord.getX();
        double deltaY = toCoord.getY() - fromCoord.getY();

        // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() calls)
        return deltaX * deltaX + deltaY * deltaY;
    }
}
