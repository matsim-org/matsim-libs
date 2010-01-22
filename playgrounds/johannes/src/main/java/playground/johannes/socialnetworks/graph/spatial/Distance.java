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
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;


/**
 * @author illenberger
 *
 */
public class Distance {

	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		Distribution distribution = new Distribution();
		
		Set<SpatialEdge> touched = new HashSet<SpatialEdge>();
		for(SpatialVertex v : vertices) {
			for(int i = 0; i < v.getEdges().size(); i++) {
				if(touched.add(v.getEdges().get(i)))
					distribution.add(v.getEdges().get(i).length());
			}
		}
		
		return distribution;
	}
	
	public Distribution vertexAccumulatedDistribution(Set<? extends SpatialVertex> vertices) {
		Distribution distribution = new Distribution();
		
		for(SpatialVertex v_i : vertices) {
			double sum = 0;
			for(SpatialEdge e : v_i.getEdges()) {
				sum += e.length();
			}
			distribution.add(sum);
		}
		
		return distribution;
	}
	
	public Distribution vertexAccumulatedCostDistribution(Collection<? extends SpatialVertex> vertices) {
		Distribution distribution = new Distribution();
		
		for(SpatialVertex v_i : vertices) {
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
