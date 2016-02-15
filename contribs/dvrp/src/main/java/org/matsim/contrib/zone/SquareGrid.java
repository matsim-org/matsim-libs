/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;


public class SquareGrid
{
    private final Network network;
    private final double cellSize;

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    private int cols;
    private int rows;

    private Zone[] zones;


    public SquareGrid(Network network, double cellSize)
    {
        this.network = network;
        this.cellSize = cellSize;

        initBounds();

        cols = (int)Math.ceil( (maxX - minX) / cellSize);
        rows = (int)Math.ceil( (maxY - minY) / cellSize);

        initZones();
    }


    //This method's content has been copied from NetworkImpl
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


    private void initZones()
    {
        zones = new Zone[rows * cols];
        double x0 = minX + cellSize / 2;
        double y0 = minY + cellSize / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                Coord coord = new Coord(c * cellSize + x0, r * cellSize + y0);
                zones[idx] = new Zone(Id.create(idx, Zone.class), "square", coord);
            }
        }
    }


    public Zone[] getZones()
    {
        return zones;
    }


    public Zone getZone(Coord coord)
    {
        int r = (int)Math.round( ( (coord.getY() - minY) / cellSize));
        int c = (int)Math.round( ( (coord.getX() - minX) / cellSize));
        return zones[r * cols + c];
    }
}
