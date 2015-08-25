package org.matsim.contrib.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;


public class DistanceUtils
{
    public static double calculateDistance(Link fromLink, Link toLink)
    {
        return calculateDistance(fromLink.getCoord(), toLink.getCoord());
    }


    public static double calculateSquaredDistance(Link fromLink, Link toLink)
    {
        return calculateSquaredDistance(fromLink.getCoord(), toLink.getCoord());
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
