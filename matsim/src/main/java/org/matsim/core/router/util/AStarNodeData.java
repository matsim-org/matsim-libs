/* *********************************************************************** *
 * project: org.matsim.*
 * AStarNodeData.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.router.util;

/**
 * Holds AStarEuclidean specific information used during routing
 * associated with each node in the network.
 */
public class AStarNodeData extends DijkstraNodeData {

	private double expectedRemainingCost;

	/**
	 * @return The expected total travel cost from the start
	 * node to the target node of the route when the associated node
	 * is on that route.
	 */
	public double getExpectedCost() {
		return this.expectedRemainingCost + getCost();
	}

	/**
	 * Sets the expected travel cost from the associated
	 * node to the target node of the route.
	 *
	 * @param expectedCost the expected cost
	 */
	public void setExpectedRemainingCost(final double expectedCost) {
		this.expectedRemainingCost = expectedCost;
	}

	/**
	 * @return The expected travel cost from the associated
	 * node to the target node of the route.
	 */
	public double getExpectedRemainingCost() {
		return this.expectedRemainingCost;
	}
}
