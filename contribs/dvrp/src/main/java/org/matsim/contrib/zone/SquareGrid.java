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

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class SquareGrid {
	private final double cellSize;

	private double minX;
	private double minY;
	private double maxX;
	private double maxY;

	private final int cols;
	private final int rows;

	private Zone[] zones;

	public SquareGrid(Collection<? extends Node> nodes, double cellSize) {
		this.cellSize = cellSize;

		initBounds(nodes);

		cols = (int)Math.ceil((maxX - minX) / cellSize);
		rows = (int)Math.ceil((maxY - minY) / cellSize);
		zones = new Zone[rows * cols];
	}

	// This method's content has been copied from NetworkImpl
	private void initBounds(Collection<? extends Node> nodes) {
		minX = Double.POSITIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		maxX = Double.NEGATIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
		for (Node n : nodes) {
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
		minX -= 1.0;// TODO use epsilon instead
		minY -= 1.0;
		maxX += 1.0;
		maxY += 1.0;
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
	}

	public Zone getZone(Coord coord) {
		int r = (int)((coord.getY() - minY) / cellSize);// == Math.floor
		int c = (int)((coord.getX() - minX) / cellSize);// == Math.floor
		return zones[r * cols + c];
	}

	public Zone getOrCreateZone(Coord coord) {
		int r = (int)((coord.getY() - minY) / cellSize);// == Math.floor
		int c = (int)((coord.getX() - minX) / cellSize);// == Math.floor
		Zone zone = zones[r * cols + c];
		if (zone == null) {
			double x0 = minX + cellSize / 2;
			double y0 = minY + cellSize / 2;
			int idx = r * cols + c;
			Coord centroid = new Coord(c * cellSize + x0, r * cellSize + y0);
			zone = new Zone(Id.create(idx, Zone.class), "square", centroid);
			zones[idx] = zone;
		}
		return zone;
	}
}
