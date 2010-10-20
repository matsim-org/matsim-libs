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

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledEdge;
import org.matsim.contrib.sna.snowball.SampledEdgeDecorator;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;

import playground.johannes.socialnetworks.graph.analysis.GraphFilter;

/**
 * @author illenberger
 *
 */
public class SampledGraphFilter<G extends Graph, V extends Vertex, E extends Edge> implements GraphFilter<SampledGraphProjection<G, V, E>> {

	private SampledGraphProjectionBuilder<G, V, E> builder = new SampledGraphProjectionBuilder<G, V, E>();
	
	private int iteration;
	
	public SampledGraphFilter(GraphBuilder<? extends SampledGraph, ? extends SampledVertex, ? extends SampledEdge> builder, int iteration) {
//		this.builder = (GraphBuilder<SampledGraph, SampledVertex, SampledEdge>) builder;
		this.iteration = iteration;
	}
	
	@Override
	public SampledGraphProjection<G, V, E> apply(SampledGraphProjection<G, V, E> graph) {
		
		Set<SampledVertexDecorator<?>> vertices = new HashSet<SampledVertexDecorator<?>>();
		
		SampledGraphProjection<G, V, E> newGraph = builder.createGraph(graph.getDelegate());
		for(SampledVertexDecorator<V> vertex : graph.getVertices()) {
			if(vertex.isDetected() && vertex.getIterationDetected() <= iteration) {
				builder.addVertex(newGraph, vertex.getDelegate());
			}
		}
		
		
		
		Set<SampledVertex> remove = new HashSet<SampledVertex>();
		
		for(SampledVertex vertex : graph.getVertices()) {
			if(!(vertex.isDetected() && vertex.getIterationDetected() <= iteration))
				remove.add(vertex);
		}
		
		for(SampledVertex vertex : remove) {
			Set<SampledEdge> edges = new HashSet<SampledEdge>(vertex.getEdges());
			for(SampledEdge edge : edges) {
				builder.removeEdge(graph, (SampledEdgeDecorator<E>) edge);
			}
			
			builder.removeVertex(graph, (SampledVertexDecorator<V>) vertex);
		}
		
		/*
		 * remove edges that have no sampled vertex
		 */
		Set<SampledEdge> removeEdges = new HashSet<SampledEdge>();
		for(SampledEdge edge : graph.getEdges()) {
			SampledVertex v1 = edge.getVertices().getFirst();
			SampledVertex v2 = edge.getVertices().getSecond();
			
			int it1 = Integer.MAX_VALUE;
			if(v1.isSampled())
				it1 = v1.getIterationSampled();
			
			int it2 = Integer.MAX_VALUE;
			if(v2.isSampled())
				it2 = v2.getIterationSampled();
			
			if(it1 > iteration && it2 > iteration)
				removeEdges.add(edge);
			
		}
		
		for(SampledEdge edge : removeEdges) {
			 builder.removeEdge(graph, (SampledEdgeDecorator<E>) edge);
		}
		return graph;
	}

}
