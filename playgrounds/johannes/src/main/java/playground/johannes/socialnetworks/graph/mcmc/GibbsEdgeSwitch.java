/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsEdgeReplace.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

/**
 * @author illenberger
 *
 */
public class GibbsEdgeSwitch extends GibbsEdgeFlip {

	private EdgeSwitchCondition switchCondition;
	
	public GibbsEdgeSwitch() {
		super();
	}

	public GibbsEdgeSwitch(long seed) {
		super(seed);
	}

	@Override
	public <V extends Vertex> void sample(AdjacencyMatrix<V> y, GraphProbability d, SampleHandler<V> handler) {
		switchCondition = new DefaultSwitchCondition();
		super.sample(y, d, handler);
	}
	
	public <V extends Vertex> void sample(AdjacencyMatrix<V> y, GraphProbability d, SampleHandler<V> handler, EdgeSwitchCondition condition) {
		switchCondition = condition;
		super.sample(y, d, handler);
	}

	@Override
	public <V extends Vertex> boolean step(AdjacencyMatrix<V> y, GraphProbability d) {
		boolean accept = false;
		int idx_ij = random.nextInt(edges.length);
		int i = edges[idx_ij][0];
		int j = edges[idx_ij][1];
		
		int u = random.nextInt(y.getVertexCount());
		int v = random.nextInt(y.getVertexCount());
		
		if(u != v && !y.getEdge(u, v)) {
			if (switchCondition.allowSwitch(y, i, j, u, v)) {
				double p_change = 1 / d.difference(y, i, j, true) * d.difference(y, u, v, false);

				double p = 1 / (1 + p_change);
				if (random.nextDouble() <= p) {
					y.removeEdge(i, j);
					y.addEdge(u, v);

					edges[idx_ij][0] = u;
					edges[idx_ij][1] = v;

					accept = true;
				}
			}
		}
		
		return accept;
	}
	
	public static class DefaultSwitchCondition implements EdgeSwitchCondition {

		@Override
		public boolean allowSwitch(AdjacencyMatrix<?> y, int i, int j, int u, int v) {
			return true;
		}
		
	}
}
