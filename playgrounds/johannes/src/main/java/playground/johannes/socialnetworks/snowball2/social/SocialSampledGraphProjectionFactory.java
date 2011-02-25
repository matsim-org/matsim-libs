/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSampledGraphProjectionFactory.java
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

import org.matsim.contrib.sna.snowball.SampledEdgeDecorator;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionFactory;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class SocialSampledGraphProjectionFactory<G extends SocialGraph, V extends SocialVertex, E extends SocialEdge> extends SampledGraphProjectionFactory<G, V, E> {

	@Override
	public SocialSampledEdgeDecorator<E> createEdge(E delegate) {
		return new SocialSampledEdgeDecorator<E>(delegate);
	}

	@Override
	public SocialSampledGraphProjection<G, V, E> createGraph(G delegate) {
		return new SocialSampledGraphProjection<G, V, E>(delegate);
	}

	@Override
	public SocialSampledVertexDecorator<V> createVertex(V delegate) {
		return new SocialSampledVertexDecorator<V>(delegate);
	}

	@Override
	public SocialSampledGraphProjection<G, V, E> copyGraph(SampledGraphProjection<G, V, E> graph) {
		return new SocialSampledGraphProjection<G, V, E>(graph.getDelegate());
	}

	@Override
	public SocialSampledVertexDecorator<V> copyVertex(SampledVertexDecorator<V> vertex) {
		SocialSampledVertexDecorator<V> copy = new SocialSampledVertexDecorator<V>(vertex.getDelegate());
		copy.sample(vertex.getIterationSampled());
		copy.detect(vertex.getIterationDetected());
		return copy;
	}

	@Override
	public SocialSampledEdgeDecorator<E> copyEdge(SampledEdgeDecorator<E> edge) {
		return new SocialSampledEdgeDecorator<E>(edge.getDelegate());
	}

}
