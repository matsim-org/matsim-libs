/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveNoCoordinates.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

/**
 * @author illenberger
 *
 */
public class RemoveNoCoordinates implements GraphFilter<SpatialGraph> {

	private GraphBuilder<SpatialGraph, SpatialVertex, SpatialEdge> builder;
	
	public RemoveNoCoordinates(GraphBuilder<? extends SpatialGraph, ? extends SpatialVertex, ? extends SpatialEdge> builder) {
		this.builder = (GraphBuilder<SpatialGraph, SpatialVertex, SpatialEdge>) builder;
	}
	
	@Override
	public SpatialGraph apply(SpatialGraph graph) {
		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		Set<SpatialEdge> edges = new HashSet<SpatialEdge>();
		
		for(SpatialVertex vertex : graph.getVertices()) {
			if(vertex.getPoint() == null) {
				vertices.add(vertex);
				edges.addAll(vertex.getEdges());
			}
		}
		
		for(SpatialEdge edge : edges) {
			builder.removeEdge(graph, edge);
		}
		
		for(SpatialVertex vertex : vertices)
			builder.removeVertex(graph, vertex);
		
		return graph;
	}

}
