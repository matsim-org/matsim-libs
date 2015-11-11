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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.*;


public class SquareGridSystem
    implements ZonalSystem
{
    public static SquareGridSystem createSquareGridSystem(Network network, double cellSize)
    {
        /////BEGIN: copied from NetworkImpl
        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for (Node n : network.getNodes().values()) {
            if (n.getCoord().getX() < minx) {
                minx = n.getCoord().getX();
            }
            if (n.getCoord().getY() < miny) {
                miny = n.getCoord().getY();
            }
            if (n.getCoord().getX() > maxx) {
                maxx = n.getCoord().getX();
            }
            if (n.getCoord().getY() > maxy) {
                maxy = n.getCoord().getY();
            }
        }
        minx -= 1.0;
        miny -= 1.0;
        maxx += 1.0;
        maxy += 1.0;
        // yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
        /////END: copied from NetworkImpl

        return createSquareGridSystem(minx, miny, cellSize, maxx, maxy);
    }


    public static SquareGridSystem createSquareGridSystem(double x0, double y0, double cellSize,
            double x1, double y1)
    {
        int cols = (int)Math.ceil( (x1 - x0) / cellSize);
        int rows = (int)Math.ceil( (y1 - y0) / cellSize);
        return new SquareGridSystem(x0, y0, cellSize, cols, rows);
    }


    private final double x0;
    private final double y0;
    private final double cellSize;
    private final int cols;
    private final int rows;

    private final Integer[][] neighbours;


    public SquareGridSystem(double x0, double y0, double cellSize, int cols, int rows)
    {
        this.x0 = x0;
        this.y0 = y0;
        this.cellSize = cellSize;
        this.cols = cols;
        this.rows = rows;

        this.neighbours = calcNeighbourLists();
    }


    private Integer[][] calcNeighbourLists()
    {
        if (cellSize < 100) {
            System.err.println("May not work as expected");
        }

        int count = rows * cols;
        Integer[][] neighbours = new Integer[count][];

        Integer[] zonesIdxs = new Integer[count];
        for (int i = 0; i < count; i++) {
            zonesIdxs[i] = i;
        }

        for (int i = 0; i < count; i++) {
            Arrays.sort(zonesIdxs, new Comparator<Integer>() {
                public int compare(Integer z1, Integer z2)
                {
                    return calcDistanceBetweenZones(z1, z2);
                }
            });

            neighbours[i] = zonesIdxs.clone();
        }
        
        return neighbours;
    }


    private int calcDistanceBetweenZones(int idx1, int idx2)
    {
        double row1 = idx1 / cols;
        double col1 = idx1 % cols;
        double row2 = idx2 / cols;
        double col2 = idx2 % cols;

        double rowRandom = (Math.random() - 0.5) * cellSize / 10;
        double colRandom = (Math.random() - 0.5) * cellSize / 10;

        double rowDelta = row1 - row2 + rowRandom;
        double colDelta = col1 - col2 + colRandom;

        return (int)Math.sqrt(rowDelta * rowDelta + colDelta * colDelta);
    }


    @Override
    public int getZoneCount()
    {
        return rows * cols;
    }


    @Override
    public int getZoneIdx(Node node)
    {
        Coord coord = node.getCoord();
        int r = (int) ( (coord.getY() - y0) % cellSize);
        int c = (int) ( (coord.getX() - x0) % cellSize);
        return r * cols + c;
    }

    
    public Iterable<Integer> getZonesIdxByDistance(Node node)
    {
        int zoneIdx = getZoneIdx(node);
        return Arrays.asList(neighbours[zoneIdx]);
    }
}
