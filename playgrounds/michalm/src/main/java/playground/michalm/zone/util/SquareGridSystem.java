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

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.*;

import playground.michalm.zone.util.SquareGridSystem.SquareZone;


public class SquareGridSystem
    implements ZonalSystem<SquareZone>
{
    public static class SquareZone
        implements ZonalSystem.Zone
    {
        private final int idx;
        private final int row;
        private final int col;


        public SquareZone(int idx, int row, int col)
        {
            this.idx = idx;
            this.row = row;
            this.col = col;
        }


        @Override
        public int getIdx()
        {
            return idx;
        }
    }


    private final Network network;
    private final double cellSize;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private final int cols;
    private final int rows;

    private final SquareZone[] zones;
    private final List<SquareZone>[] zonesByDistance;


    public SquareGridSystem(Network network, double cellSize)
    {
        this.network = network;
        this.cellSize = cellSize;

        initBounds();

        cols = (int)Math.ceil( (maxX - minX) / cellSize);
        rows = (int)Math.ceil( (maxY - minY) / cellSize);

        zones = initZones();

        zonesByDistance = ZonalSystems.initZonesByDistance(this, network, zones,
                new ZonalSystems.DistanceCalculator<SquareZone>() {
                    public double calcDistance(SquareZone z1, SquareZone z2)
                    {
                        return (z1.row - z2.row) * (z1.row - z2.row) + (z1.col - z2.col) * (z1.col - z2.col);
                    }
                });
    }


    //The content is copied from NetworkImpl
    private void initBounds()
    {
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
        for (Node n : network.getNodes().values()) {
            if (n.getCoord().getX() < minX) {
                minX = n.getCoord().getX();
            }
            if (n.getCoord().getY() < minY) {
                minY = n.getCoord().getY();
            }
            if (n.getCoord().getX() > maxX) {
                maxX = n.getCoord().getX();
            }
            if (n.getCoord().getY() > maxY) {
                maxY = n.getCoord().getY();
            }
        }
        minX -= 1.0;
        minY -= 1.0;
        maxX += 1.0;
        maxY += 1.0;
        // yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
    }


    private SquareZone[] initZones()
    {
        SquareZone[] zones = new SquareZone[rows * cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                zones[idx] = new SquareZone(idx, r, c);
            }
        }

        return zones;
    }


    @Override
    public SquareZone getZone(Node node)
    {
        Coord coord = node.getCoord();
        int r = (int)Math.round( ( (coord.getY() - minY) / cellSize));
        int c = (int)Math.round( ( (coord.getX() - minX) / cellSize));
        return zones[r * cols + c];
    }


    @Override
    public int getZoneCount()
    {
        return zones.length;
    }


    @Override
    public Iterable<SquareZone> getZonesByDistance(Node node)
    {
        return zonesByDistance[getZone(node).getIdx()];
    }
}
