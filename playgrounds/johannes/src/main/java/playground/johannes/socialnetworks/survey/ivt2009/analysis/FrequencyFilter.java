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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

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
