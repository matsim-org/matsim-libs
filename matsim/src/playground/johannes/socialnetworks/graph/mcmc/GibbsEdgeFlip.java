/* *********************************************************************** *
 * project: org.matsim.*
 * GibbsEdgeSwitcher.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.mcmc;


/**
 * @author illenberger
 *
 */
public class GibbsEdgeFlip extends GibbsSampler {

	protected int[][] edges;
	
	public GibbsEdgeFlip() {
		super();
	}

	public GibbsEdgeFlip(long seed) {
		super(seed);
	}

	@Override
	public void sample(AdjacencyMatrix y, ConditionalDistribution d, SampleHandler handler) {
		int N = y.getVertexCount();
		int M = y.getEdgeCount();
		edges = new int[M][2];
		int idx_ij = 0;
		for(int i = 0; i < N; i++) {
			for(int j = i+1; j < N; j++) {
				if(y.getEdge(i, j)) {
					edges[idx_ij][0] = i;
					edges[idx_ij][1] = j;
					idx_ij++;
				}
			}
		}
//		super.sample(y, d, burninTime, handler);
		super.sample(y, d, handler);
	}
	
	@Override
	public boolean step(AdjacencyMatrix y, ConditionalDistribution d) {
		boolean accept = false;
		int idx_ij = random.nextInt(edges.length);
		int i = edges[idx_ij][0];
		int j = edges[idx_ij][1];
		
		int idx_uv = random.nextInt(edges.length);
		while(idx_uv == idx_ij) {
			idx_uv = random.nextInt(edges.length);
		}
		
		int u = edges[idx_uv][0];
		int v = edges[idx_uv][1];
		
		if(i != u && j != v && j != u && i != v && !y.getEdge(i, u) && !y.getEdge(j, v)) {
			
			double p_change = d.changeStatistic(y, i, u, false)
					* d.changeStatistic(y, j, v, false)
					* 1/d.changeStatistic(y, i, j, true)
					* 1/d.changeStatistic(y, u, v, true); 
			double p = 1 / (1 + p_change);
			if(random.nextDouble() <= p) {
				y.removeEdge(i, j);
				y.removeEdge(u, v);
				y.addEdge(i, u);
				y.addEdge(j, v);
				
				edges[idx_ij][0] = i;
				edges[idx_ij][1] = u;
				edges[idx_uv][0] = j;
				edges[idx_uv][1] = v;
				
				accept = true;
			}
		}
		
		return accept;
	}
}
