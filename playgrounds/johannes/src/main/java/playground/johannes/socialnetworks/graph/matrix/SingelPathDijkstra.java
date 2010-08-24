/* *********************************************************************** *
 * project: org.matsim.*
 * Dijkstra.java
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
package playground.johannes.socialnetworks.graph.matrix;

import gnu.trove.TIntArrayList;

import java.util.Arrays;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;


/**
 * @author illenberger
 *
 */
public class SingelPathDijkstra extends DijkstraEngine {

	private int[] spanningTree;
	
	private EdgeCostFunction weights;
	
	public SingelPathDijkstra(AdjacencyMatrix<?> y, EdgeCostFunction weights) {
		super(y);
		this.weights = weights;
	}
	
	public TIntArrayList run(int source, int target) {
		spanningTree = new int[y.getVertexCount()];
		Arrays.fill(spanningTree, -1);
		return super.run(source, target);
	}
	
	@Override
	protected void foundCheaper(int i, int j, double cost) {
		spanningTree[j] = i;
	}

	@Override
	protected double getCost(int i, int j) {
		return weights.edgeCost(i, j);
	}

	public TIntArrayList getPath(int i, int j) {
		TIntArrayList path = new TIntArrayList();
		
		if(i == j)
			return path;
		
		if(spanningTree[j] == -1)
			return null;
		
		while(j != i) {
			path.add(j);
			if(spanningTree[j] > -1)
				j = spanningTree[j];
			else
				return null;
		}
		path.reverse();
		
		return path;
	}
	
	public int[] getSpanningTree() {
		return spanningTree;
	}
}
