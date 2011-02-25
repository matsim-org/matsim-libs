/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptPropConst2.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.graph.spatial.analysis.AbstractSpatialProperty;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class AcceptPropConst2 extends AbstractSpatialProperty {

	private final TObjectDoubleHashMap<Vertex> accessibility;
	
	public AcceptPropConst2(TObjectDoubleHashMap<Vertex> accessibility) {
		this.accessibility = accessibility;
	}
	
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		Set<? extends SpatialVertex> spatialVertices = (Set<? extends SpatialVertex>) vertices;
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>();

		for (SpatialVertex vertex : spatialVertices) {
			Point p1 = vertex.getPoint();
			if (p1 != null) {				
				values.put(vertex, accessibility.get(vertex)/(double)vertex.getEdges().size());
			}
		}
		
		return values;
	}

}
