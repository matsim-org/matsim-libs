/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.matrix;

/**
 * @author illenberger
 *
 */
public class WeightedDijkstra extends Dijkstra {

	private EdgeCostFunction costs;
	
	public WeightedDijkstra(AdjacencyMatrix<?> y, EdgeCostFunction costs) {
		super(y);
		this.costs = costs;
	}

	@Override
	protected double getCost(int i, int j) {
		return costs.edgeCost(i, j);
	}

}
