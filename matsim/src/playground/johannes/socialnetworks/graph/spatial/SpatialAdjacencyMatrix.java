/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAdjacencyMatrix.java
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
package playground.johannes.socialnetworks.graph.spatial;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixDecorator;

/**
 * @author illenberger
 *
 */
public class SpatialAdjacencyMatrix extends AdjacencyMatrixDecorator<SpatialSparseVertex> {
	
	public SpatialAdjacencyMatrix(SpatialSparseGraph g) {
		super(g);
	}

	public SpatialSparseGraph getGraph() {
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder();
		SpatialSparseGraph g = new SpatialSparseGraph();

		TIntObjectHashMap<SpatialSparseVertex> vertexIdx = new TIntObjectHashMap<SpatialSparseVertex>();
		for (int i = 0; i < getVertexCount(); i++) {
			SpatialSparseVertex ego = builder.addVertex(g, getVertex(i).getCoordinate());
			vertexIdx.put(i, ego);
		}

		for (int i = 0; i < getVertexCount(); i++) {
			TIntArrayList row = getNeighbours(i);
			if (row != null) {
				for (int idx = 0; idx < row.size(); idx++) {
					int j = row.get(idx);
					if (j > i) {
						if (builder.addEdge(g, vertexIdx.get(i), vertexIdx
								.get(j)) == null)
							throw new RuntimeException();
					}
				}
			}
		}

		return g;
	}
}
