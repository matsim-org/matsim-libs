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

package org.matsim.core.network.algorithms;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;

/**
 * Calculates the bounding box (=maximal extent) of a network.
 *
 * @author mrieser
 */
public final class CalcBoundingBox implements NetworkRunnable {

	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;

	@Override
	public void run(final Network network) {
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
