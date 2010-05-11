/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeCosts.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.spatial.generators.EdgeCostFunction;

/**
 * @author illenberger
 *
 */
public class EdgeCosts {

	private EdgeCostFunction costFunction;
	
	public EdgeCosts(EdgeCostFunction costFunction) {
		this.costFunction = costFunction;
	}
	
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		Distribution distribution = new Distribution();
		
		Set<SpatialEdge> touched = new HashSet<SpatialEdge>();
		for(SpatialVertex v : vertices) {
			for(int i = 0; i < v.getEdges().size(); i++) {
				if(touched.add(v.getEdges().get(i)))
					distribution.add(costFunction.edgeCost(v, v.getNeighbours().get(i)));
			}
		}
		
		return distribution;
	}
	
	public Distribution vertexCostsSum(Set<? extends SpatialVertex> vertices) {
		Distribution distr = new Distribution();
		for(SpatialVertex vertex : vertices) {
			double sum = 0;
			for(SpatialVertex neighbor : vertex.getNeighbours()) {
				sum += costFunction.edgeCost(vertex, neighbor);
			}
			
			distr.add(sum);
		}
		
		return distr;
	}
}
