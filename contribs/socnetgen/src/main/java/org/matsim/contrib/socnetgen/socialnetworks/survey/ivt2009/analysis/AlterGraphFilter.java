/* *********************************************************************** *
 * project: org.matsim.*
 * AlterGraphFilter.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis;

import org.matsim.contrib.socnetgen.sna.graph.GraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphFilter;
import org.matsim.contrib.socnetgen.sna.snowball.SampledEdge;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;

import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class AlterGraphFilter implements GraphFilter<SampledGraph> {

	private final GraphBuilder<SampledGraph, SampledVertex, SampledEdge> builder;
	
	public AlterGraphFilter(GraphBuilder<? extends SampledGraph, ? extends SampledVertex, ? extends SampledEdge> builder) {
		this.builder = (GraphBuilder<SampledGraph, SampledVertex, SampledEdge>) builder;
	}
	
	@Override
	public SampledGraph apply(SampledGraph graph) {
		SampledGraph copy = builder.copyGraph(graph);
		
		Set<SampledVertex> removeVertex = new HashSet<SampledVertex>();
		Set<SampledEdge> removeEdge = new HashSet<SampledEdge>();
		
		for(SampledVertex v : copy.getVertices()) {
			if(!v.isSampled()) {
				Set<SampledVertex> seeds = new HashSet<SampledVertex>();
				for(SampledVertex neighbour : v.getNeighbours()) {
					seeds.add(neighbour.getSeed());
				}
				
				if(seeds.size() <= 1) {
					removeVertex.add(v);
					removeEdge.addAll(v.getEdges());
				}
			}
		}
		
		for(SampledEdge edge : removeEdge) {
			builder.removeEdge(copy, edge);
		}
		
		for(SampledVertex vertex : removeVertex) {
			builder.removeVertex(copy, vertex);
		}
		
		return copy;
	}
	
	

}
