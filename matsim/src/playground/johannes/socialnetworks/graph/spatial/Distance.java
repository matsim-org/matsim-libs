/* *********************************************************************** *
 * project: org.matsim.*
 * Distance.java
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;

import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class Distance<V extends SpatialVertex> {

	public Distribution distribution(Collection<V> vertices) {
		Distribution distribution = new Distribution();
		
		Set<V> pending = new HashSet<V>(vertices);
		for(V v : vertices) {
			for(int i = 0; i < v.getEdges().size(); i++) {
				SpatialVertex v_j = v.getNeighbours().get(i);
				if(pending.contains(v_j))
					distribution.add(((SpatialEdge) v.getEdges().get(i)).length());
			}
			
			pending.remove(v);
		}
		
		return distribution;
	}
	
	public Distribution vertexAccumulatedDistribution(Collection<? extends V> vertices) {
		Distribution distribution = new Distribution();
		
		for(V v_i : vertices) {
			double sum = 0;
			for(Edge e : v_i.getEdges()) {
				sum += ((SpatialEdge) e).length();
			}
			distribution.add(sum);
		}
		
		return distribution;
	}
	
	public Distribution vertexAccumulatedCostDistribution(Collection<? extends V> vertices) {
		Distribution distribution = new Distribution();
		
		for(V v_i : vertices) {
			double sum = 0;
			for(Edge e : v_i.getEdges()) {
				double d = ((SpatialEdge) e).length();
				d= Math.ceil(d/1000.0);
				d=Math.max(1,d);
//				sum += 15 * Math.log(d/2.0 + 1);
				sum += Math.log(d);
			}
			distribution.add(sum);
		}
		
		return distribution;
	}
}
