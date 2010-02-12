/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphProjectionFactory.java
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
package playground.johannes.socialnetworks.snowball2;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphProjectionFactory;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class SampledGraphProjectionFactory<G extends Graph, V extends Vertex, E extends Edge> implements
		GraphProjectionFactory<G, V, E, SampledGraphProjection<G, V, E>, SampledVertexDecorator<V>, SampledEdgeDecorator<E>> {

	@Override
	public SampledEdgeDecorator<E> createEdge(E delegate) {
		return new SampledEdgeDecorator<E>(delegate);
	}

	@Override
	public SampledGraphProjection<G, V, E> createGraph(G delegate) {
		return new SampledGraphProjection<G, V, E>(delegate);
	}

	@Override
	public SampledVertexDecorator<V> createVertex(V delegate) {
		return new SampledVertexDecorator<V>(delegate);
	}

}
