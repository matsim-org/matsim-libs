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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;


/**
 * @author illenberger
 *
 */
public class GibbsEdgeFlip extends GibbsSampler {

	protected int[][] edges;
	
	private EdgeSwitchCondition switchCondition;
	
	public GibbsEdgeFlip() {
		super();
	}

	public GibbsEdgeFlip(long seed) {
		super(seed);
	}

	public <V extends Vertex> void sample(AdjacencyMatrix<V> y, GraphProbability d, SamplerListener<V> handler, EdgeSwitchCondition condition) {
		switchCondition = condition;
		sample(y, d, handler);
	}
	
	@Override
	public <V extends Vertex> void sample(AdjacencyMatrix<V> y, GraphProbability d, SamplerListener<V> handler) {
		if(switchCondition == null)
			switchCondition = new GibbsEdgeSwitch.DefaultSwitchCondition();
		
		int N = y.getVertexCount();
		int M = y.countEdges();
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
		super.sample(y, d, handler);
	}
	
	@Override
	public <V extends Vertex> boolean step(AdjacencyMatrix<V> y, GraphProbability d) {
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
			
			if(switchCondition.allowSwitch(y, i, j, i, u) && switchCondition.allowSwitch(y, u, v, j, v)) {
			/*
			 * ************************************************
			 * TODO: CHECK THIS!
			 */
			double p_change = d.difference(y, i, u, false)
					* d.difference(y, j, v, false)
					* 1/d.difference(y, i, j, true)
					* 1/d.difference(y, u, v, true); 
			double p = 1 / (1 + p_change);
			/*
			 * ************************************************
			 */
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
		}
		
		return accept;
	}
}
