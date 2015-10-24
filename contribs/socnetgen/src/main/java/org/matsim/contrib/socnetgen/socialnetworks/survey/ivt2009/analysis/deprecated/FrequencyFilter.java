/* *********************************************************************** *
 * project: org.matsim.*
 * FrequencyFilter.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis.deprecated;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.GraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphFilter;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;

import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class FrequencyFilter implements GraphFilter<SocialGraph> {

	private GraphBuilder<SocialGraph, SocialVertex, SocialEdge> builder;
	
	private double threshold;
	
	public FrequencyFilter(GraphBuilder<? extends Graph, ? extends Vertex, ? extends Edge> builder, double threshold) {
		this.builder = (GraphBuilder<SocialGraph, SocialVertex, SocialEdge>) builder;
		this.threshold = threshold;
	}
	
	@Override
	public SocialGraph apply(SocialGraph graph) {
		Set<SocialEdge> remove = new HashSet<SocialEdge>();
		
		for(SocialEdge edge : graph.getEdges()) {
			if(edge.getFrequency() > threshold) {
				remove.add(edge);
			}
		}
		
		for(SocialEdge edge : remove)
			builder.removeEdge(graph, edge);
		
		return graph;
	}

}
