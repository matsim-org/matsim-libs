/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphFilter.java
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

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.snowball.SampledEdge;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

/**
 * @author illenberger
 * 
 */
public class SampledGraphFilter implements GraphFilter<SampledGraph> {

	private static final Logger logger = Logger.getLogger(SampledGraphFilter.class);
	
	private GraphBuilder<SampledGraph, SampledVertex, SampledEdge> builder;

	private int iteration;

	public SampledGraphFilter(GraphBuilder<? extends SampledGraph, ? extends SampledVertex, ? extends SampledEdge> builder, int iteration) {
		this.builder = (GraphBuilder<SampledGraph, SampledVertex, SampledEdge>) builder;
		this.iteration = iteration;
	}

	@Override
	public SampledGraph apply(SampledGraph graph) {
		SampledGraph copy = builder.copyGraph(graph);
		/*
		 * remove edges that have no sampled vertex
		 */
		Set<SampledEdge> removeEdges = new HashSet<SampledEdge>();
		for (SampledEdge edge : copy.getEdges()) {
			SampledVertex v1 = edge.getVertices().getFirst();
			SampledVertex v2 = edge.getVertices().getSecond();

			int it1 = Integer.MAX_VALUE;
			if (v1.isSampled())
				it1 = v1.getIterationSampled();

			int it2 = Integer.MAX_VALUE;
			if (v2.isSampled())
				it2 = v2.getIterationSampled();

			if (it1 > iteration && it2 > iteration)
				removeEdges.add(edge);

		}

		for (SampledEdge edge : removeEdges) {
			builder.removeEdge(copy, edge);
		}
		/*
		 * remove vertices
		 */
		Set<SampledVertex> removeVertices = new HashSet<SampledVertex>();
		for (SampledVertex vertex : copy.getVertices()) {
			if (!(vertex.isDetected() && vertex.getIterationDetected() <= iteration))
				removeVertices.add(vertex);
		}

		for (SampledVertex vertex : removeVertices) {
			if(!builder.removeVertex(copy, vertex)) {
				Set<SampledEdge> removeEdges2 = new HashSet<SampledEdge>();
				for(SampledEdge edge : vertex.getEdges()) {
					removeEdges2.add(edge);
				}
				for(SampledEdge edge : removeEdges2) {
					logger.debug(String.format("Force-removing edge from (%1$s) to (%2$s).", vertex.getIterationSampled(), edge.getOpposite(vertex).getIterationSampled()));
					builder.removeEdge(copy, edge);
				}
				
				if(!builder.removeVertex(copy, vertex)) {
					throw new RuntimeException("Something wired happened...");
				}
			}
		}
		/*
		 * mark the leafs of the snowball tree as unsampled. 
		 */
		for (SampledVertex vertex : copy.getVertices()) {
			if(vertex.getIterationDetected() == iteration)
				vertex.sample(null);
		}
		
		return copy;
	}

}
