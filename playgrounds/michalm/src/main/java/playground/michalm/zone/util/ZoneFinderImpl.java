/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.zone.util;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import playground.michalm.zone.Zone;


public class ZoneFinderImpl
    implements ZoneFinder
{
    private final SpatialIndex quadTree = new Quadtree();
    private final double expansionDistance;


    public ZoneFinderImpl(Map<Id<Zone>, Zone> zones, double expansionDistance)
    {
        this.expansionDistance = expansionDistance;

        for (Zone z : zones.values()) {
            quadTree.insert(z.getMultiPolygon().getEnvelopeInternal(), z);
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public Zone findZone(Coord coord)
    {
        Point point = MGC.coord2Point(coord);
        Envelope env = point.getEnvelopeInternal();

        Zone zone = getSmallestZoneContainingPoint(quadTree.query(env), point);
        if (zone != null) {
            return zone;
        }

        if (expansionDistance > 0) {
            env.expandBy(expansionDistance);
            zone = getNearestZone(quadTree.query(env), point);
        }

        return zone;
    }


    private Zone getSmallestZoneContainingPoint(List<Zone> zones, Point point)
    {
        if (zones.size() == 1) {//almost 100% cases
            return zones.get(0);
        }

        double minArea = Double.MAX_VALUE;
        Zone smallestZone = null;

        for (Zone z : zones) {
            if (z.getMultiPolygon().contains(point)) {
                double area = z.getMultiPolygon().getArea();
                if (area < minArea) {
                    minArea = area;
                    smallestZone = z;
                }
            }
        }

        return smallestZone;
    }


    private Zone getNearestZone(List<Zone> zones, Point point)
    {
        if (zones.size() == 1) {
            return zones.get(0);
        }

        double minDistance = Double.MAX_VALUE;
        Zone nearestZone = null;

        for (Zone z : zones) {
            double distance = z.getMultiPolygon().distance(point);
            if (distance <= expansionDistance) {
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestZone = z;
                }
            }
        }

        return nearestZone;
    }

}
