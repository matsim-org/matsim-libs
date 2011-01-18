/* *********************************************************************** *
 * project: org.matsim.*
 * ConserveDegreeDistribution.java
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
package playground.johannes.socialnetworks.graph.mcmc;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class ConserveDegreeDistribution implements EdgeSwitchCondition {

	@Override
	public boolean allowSwitch(AdjacencyMatrix<?> y, int i, int j, int u, int v) {
		int k_i = y.getNeighborCount(i);
		int k_j = y.getNeighborCount(j);
		int k_u = y.getNeighborCount(u) + 1;
		int k_v = y.getNeighborCount(v) + 1;
		
		boolean cond1 = k_i == k_u && k_j == k_v;
		boolean cond2 = k_j == k_u && k_i == k_v;
		
		return cond1 || cond2;
	}

}
