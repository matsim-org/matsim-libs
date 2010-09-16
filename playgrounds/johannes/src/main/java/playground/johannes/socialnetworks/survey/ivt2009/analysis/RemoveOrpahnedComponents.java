/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveOrpahnedComponents.java
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
import java.util.List;
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
public class RemoveOrpahnedComponents implements GraphFilter<SampledGraph> {

	private GraphBuilder<SampledGraph, SampledVertex, SampledEdge> builder;
	
	public void setBuilder(GraphBuilder<? extends SampledGraph, ? extends SampledVertex, ? extends SampledEdge> builder) {
		this.builder = (GraphBuilder<SampledGraph, SampledVertex, SampledEdge>) builder;
	}
	
	@Override
	public SampledGraph apply(SampledGraph graph) {
		ComponentValidator validator = new ComponentValidator();
		validator.validate(graph);
		
		List<Set<SampledVertex>> comps = validator.getOrphandComponents();
		for(Set<SampledVertex> comp : comps) {
			for(SampledVertex vertex : comp) {
				Set<SampledEdge> edges = new HashSet<SampledEdge>(vertex.getEdges());
				for(SampledEdge edge : edges) {
					builder.removeEdge(graph, edge);
				}
				
				builder.removeVertex(graph, vertex);
			}
		}
		
		return graph;
	}

}
