/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.zone;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.common.collect.Queues;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;


public class ZoneFinder
{
    private static class ZoneEntry
        implements Comparable<ZoneEntry>
    {
        private final Zone zone;
        private final double weight;


        private ZoneEntry(Zone zone, double weight)
        {
            this.zone = zone;
            this.weight = weight;
        }


        @Override
        public int compareTo(ZoneEntry o)
        {
            return Double.compare(weight, o.weight);
        }
    }


    private final SpatialIndex quadTree = new Quadtree();
    private final double maxDistance;

    private Point point;
    private Queue<ZoneEntry> queue;


    public ZoneFinder(Map<Id<Zone>, Zone> zones, double maxDistance)
    {
        this.maxDistance = maxDistance;

        for (Zone z : zones.values()) {
            quadTree.insert(z.getMultiPolygon().getEnvelopeInternal(), z);
        }
    }


    @SuppressWarnings("unchecked")
    public Zone findZone(Coord coord)
    {
        point = MGC.coord2Point(coord);
        queue = Queues.newPriorityQueue();

        Envelope env = point.getEnvelopeInternal();
        queueByArea(quadTree.query(env));

        if (queue.size() > 1) {
            Zone result = queue.peek().zone;
            printOutQueue();
            return result;
        }

        if (queue.isEmpty() && maxDistance > 0) {
            env.expandBy(maxDistance);
            queueByDistance(quadTree.query(env));
        }

        if (queue.isEmpty()) {
            return null;
        }

        return queue.peek().zone;
    }


    private void queueByArea(List<Zone> candidateZones)
    {
        for (Zone z : candidateZones) {
            if (z.getMultiPolygon().contains(point)) {
                queue.add(new ZoneEntry(z, z.getMultiPolygon().getArea()));
            }
        }
    }


    private void queueByDistance(List<Zone> candidateZones)
    {
        for (Zone z : candidateZones) {
            double distance = z.getMultiPolygon().distance(point);
            if (distance <= maxDistance) {
                queue.add(new ZoneEntry(z, distance));
            }
        }
    }


    private void printOutQueue()
    {
        StringBuilder sb = new StringBuilder().append(queue.poll().zone.getId());

        do {
            sb.append(", ").append(queue.poll().zone.getId());
        }
        while (!queue.isEmpty());

        System.err.println("Overlaying zones: " + sb.toString());
    }
}
