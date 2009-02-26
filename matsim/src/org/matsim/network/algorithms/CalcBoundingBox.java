/* *********************************************************************** *
 * project: org.matsim.*
 * CalcBoundingBox.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.network.algorithms;

import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;

/**
 * Calculates the bounding box (=maximal extent) of a network.
 *
 * @author mrieser
 */
public class CalcBoundingBox {

	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;

	public void run(final NetworkLayer network) {
		this.minX = Double.POSITIVE_INFINITY;
		this.minY = Double.POSITIVE_INFINITY;
		this.maxX = Double.NEGATIVE_INFINITY;
		this.maxY = Double.NEGATIVE_INFINITY;
		for (Node n : network.getNodes().values()) {
			if (n.getCoord().getX() < this.minX) { this.minX = n.getCoord().getX(); }
			if (n.getCoord().getY() < this.minY) { this.minY = n.getCoord().getY(); }
			if (n.getCoord().getX() > this.maxX) { this.maxX = n.getCoord().getX(); }
			if (n.getCoord().getY() > this.maxY) { this.maxY = n.getCoord().getY(); }
		}
	}

	public double getMinX() {
		return this.minX;
	}

	public double getMaxX() {
		return this.maxX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxY() {
		return this.maxY;
	}

}
