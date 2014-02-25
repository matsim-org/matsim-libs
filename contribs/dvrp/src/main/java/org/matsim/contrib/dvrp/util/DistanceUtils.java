package org.matsim.contrib.dvrp.util;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;


public class DistanceUtils
{
    public static double calculateSquareDistance(Link fromLink, Link toLink)
    {
        return calculateSquareDistance(fromLink.getCoord(), toLink.getCoord());
    }


    public static double calculateSquareDistance(Coord fromCoord, Coord toCoord)
    {
        double deltaX = toCoord.getX() - fromCoord.getX();
        double deltaY = toCoord.getY() - fromCoord.getY();

        // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() calls)
        return deltaX * deltaX + deltaY * deltaY;
    }
}
