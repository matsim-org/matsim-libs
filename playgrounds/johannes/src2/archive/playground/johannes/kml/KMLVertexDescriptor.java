/* *********************************************************************** *
 * project: org.matsim.*
 * KMLVertexDescriptor.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;

import gnu.trove.TObjectDoubleHashMap;
import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;

/**
 * @author illenberger
 *
 */
public class KMLVertexDescriptor implements KMLObjectDescriptor<SpatialSparseVertex> {
	
	private TObjectDoubleHashMap<SpatialSparseVertex> clustering;
	
	private TObjectDoubleHashMap<SpatialSparseVertex> meanEdgeLength;
	
	@SuppressWarnings("unchecked")
	public KMLVertexDescriptor(SpatialSparseGraph graph) {
		clustering = (TObjectDoubleHashMap<SpatialSparseVertex>) GraphStatistics.localClusteringCoefficients(graph);
		meanEdgeLength = (TObjectDoubleHashMap<SpatialSparseVertex>) SpatialGraphStatistics.meanEdgeLength(graph);
	}
	
	public String getDescription(SpatialSparseVertex object) {
		StringBuilder builder= new StringBuilder();
		
		builder.append("k = ");
		builder.append(String.valueOf(object.getNeighbours().size()));
		builder.append("<br>");
		builder.append("c = ");
		builder.append(String.valueOf(clustering.get(object)));
		builder.append("<br>");
		builder.append("d = ");
		builder.append(String.valueOf(meanEdgeLength.get(object)));
		
		return builder.toString();
	}

	public String getName(SpatialSparseVertex object) {
		return null;
	}

}
