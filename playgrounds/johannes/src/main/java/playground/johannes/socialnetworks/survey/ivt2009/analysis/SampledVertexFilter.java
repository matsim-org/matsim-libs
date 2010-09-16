/* *********************************************************************** *
 * project: org.matsim.*
 * SampledVertexFilter.java
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
import org.matsim.contrib.sna.snowball.SampledEdge;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

/**
 * @author illenberger
 *
 */
public class SampledVertexFilter implements GraphFilter<SampledGraph> {

	private GraphBuilder<SampledGraph, SampledVertex, SampledEdge> builder;
	
	public SampledVertexFilter(GraphBuilder<? extends SampledGraph, ? extends SampledVertex, ? extends SampledEdge> builder) {
		this.builder = (GraphBuilder<SampledGraph, SampledVertex, SampledEdge>) builder;
	}
	
	@Override
	public SampledGraph apply(SampledGraph graph) {
		Set<SampledVertex> remove = new HashSet<SampledVertex>();
		
		for(SampledVertex vertex : graph.getVertices()) {
			if(!vertex.isSampled())
				remove.add(vertex);
		}
		
		for(SampledVertex vertex : remove) {
			Set<SampledEdge> edges = new HashSet<SampledEdge>(vertex.getEdges());
			for(SampledEdge edge : edges) {
				builder.removeEdge(graph, edge);
			}
			
			builder.removeVertex(graph, vertex);
		}
		
		return graph;
	}

}
