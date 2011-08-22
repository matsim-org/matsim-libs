/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeLength.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculatorFactory;
import playground.johannes.socialnetworks.graph.analysis.AbstractEdgeProperty;

/**
 * @author illenberger
 *
 */
public class EdgeLength extends AbstractEdgeProperty {

	private static EdgeLength instance;
	
	public static EdgeLength getInstance() {
		if(instance == null)
			instance = new EdgeLength();
		return instance;
	}
	
	private DistanceCalculator distanceCalculator;
	
	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.distanceCalculator = calculator;
	}
	
	@Override
	public TObjectDoubleHashMap<Edge> values(Set<? extends Edge> edges) {
		@SuppressWarnings("unchecked")
		Set<? extends SpatialEdge> spatialEdges = (Set<? extends SpatialEdge>) edges;
		
		TObjectDoubleHashMap<Edge> values = new TObjectDoubleHashMap<Edge>();
		for(SpatialEdge edge : spatialEdges) {
			SpatialVertex v_i = edge.getVertices().getFirst();
			SpatialVertex v_j = edge.getVertices().getSecond();
			
			if(v_i.getPoint() != null && v_j.getPoint() != null) {
				if(v_i.getPoint().getSRID() == v_j.getPoint().getSRID()) {
					if(distanceCalculator == null) {
						distanceCalculator = DistanceCalculatorFactory.createDistanceCalculator(CRSUtils.getCRS(v_i.getPoint().getSRID()));
					}
					
					double d = distanceCalculator.distance(v_i.getPoint(), v_j.getPoint());
					d = Math.max(d, 300);
					values.put(edge, d);
				} else {
					throw new RuntimeException("Points do not share the same coordinate reference system.");
				}
			}
		}
		
		return values;
	}

}
