/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSampledGraphProjectionBuilder.java
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
package playground.johannes.socialnetworks.snowball2.social;

import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.sna.snowball.SampledEdgeDecorator;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class SocialSampledGraphProjectionBuilder<G extends SocialGraph, V extends SocialVertex, E extends SocialEdge> extends SampledGraphProjectionBuilder<G, V, E> {

	public SocialSampledGraphProjectionBuilder() {
		super(new SocialSampledGraphProjectionFactory<G, V, E>());
	}

	/*
	 * UNTESTED!
	 */
	@Override
	public SocialSampledGraphProjection<G, V, E> copyGraph(SampledGraphProjection<G, V, E> graph) {
		SocialSampledGraphProjection<G, V, E> newGraph = (SocialSampledGraphProjection<G, V, E>) getProjectionFactory().copyGraph(graph);
		
		Map<SampledVertexDecorator<V>, SocialSampledVertexDecorator<V>> vertexMapping = new HashMap<SampledVertexDecorator<V>, SocialSampledVertexDecorator<V>>();
		
		for(SampledVertexDecorator<V> vertex : graph.getVertices()) {
			SocialSampledVertexDecorator<V> newVertex = (SocialSampledVertexDecorator<V>) getProjectionFactory().copyVertex((SampledVertexDecorator<V>) vertex);
			vertexMapping.put(vertex, newVertex);
			if(!insertVertex(newGraph, newVertex))
				throw new RuntimeException("Could not insert vertex into graph.");
		}
		
		for(SampledVertexDecorator<V> vertex : graph.getVertices()) {
			SampledVertexDecorator<V> seed = vertex.getSeed();
			SocialSampledVertexDecorator<V> vertexCopy = vertexMapping.get(vertex);
			SocialSampledVertexDecorator<V> seedCopy = vertexMapping.get(seed);
			vertexCopy.setSeed(seedCopy);
		}
		
		for(SampledEdgeDecorator<E> edge : graph.getEdges()) {
			SocialSampledVertexDecorator<V> vertex1 = vertexMapping.get(edge.getVertices().getFirst());
			SocialSampledVertexDecorator<V> vertex2 = vertexMapping.get(edge.getVertices().getSecond());
			SocialSampledEdgeDecorator<E> newEdge = (SocialSampledEdgeDecorator<E>) getProjectionFactory().copyEdge(edge);
			if(!insertEdge(newGraph, vertex1, vertex2, newEdge))
				throw new RuntimeException("Could not edge vertex into graph.");
		}
		
		return newGraph;
	}
}
